package com.rakshaklogix.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class LogisticsController {
    private final JdbcTemplate db;
    public LogisticsController(JdbcTemplate db) { this.db = db; }

    @GetMapping("/dashboard/summary")
    public Map<String, Object> summary() {
        return Map.of("inventoryItems", count("SELECT COUNT(*) FROM inventory_items"),
                "availableStock", count("SELECT COALESCE(SUM(available_quantity),0) FROM inventory_items"),
                "lowStockItems", count("SELECT COUNT(*) FROM inventory_items WHERE available_quantity <= minimum_stock_threshold"),
                "pendingRequests", count("SELECT COUNT(*) FROM supply_requests WHERE status='Pending'"),
                "equipmentUnderMaintenance", count("SELECT COUNT(*) FROM equipment WHERE service_status='Under Maintenance'"),
                "upcomingMaintenance", count("SELECT COUNT(*) FROM maintenance_records WHERE status='Scheduled' AND scheduled_date >= NOW()"));
    }
    private long count(String sql) { Long value = db.queryForObject(sql, Long.class); return value == null ? 0 : value; }

    @GetMapping("/inventory")
    public List<Map<String,Object>> inventory(@RequestParam(required=false) String search, @RequestParam(required=false) String category) {
        String q = "SELECT id,item_id AS itemId,name,category,description,available_quantity AS availableQuantity,minimum_stock_threshold AS minimumStockThreshold,unit_of_measure AS unitOfMeasure,storage_location AS storageLocation,supplier,item_status AS status,last_updated AS lastUpdated FROM inventory_items WHERE (? IS NULL OR name LIKE CONCAT('%',?,'%') OR item_id LIKE CONCAT('%',?,'%')) AND (? IS NULL OR category=?) ORDER BY name";
        return db.queryForList(q, search, search, search, category, category);
    }
    public record ItemRequest(@NotBlank String itemId, @NotBlank String name, @NotBlank String category,
                              @Min(0) int availableQuantity, @Min(0) int minimumStockThreshold,
                              @NotBlank String unitOfMeasure, String description, String storageLocation, String supplier) {}
    @PostMapping("/inventory")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> createItem(@Valid @RequestBody ItemRequest r) {
        db.update("INSERT INTO inventory_items(item_id,name,category,description,available_quantity,minimum_stock_threshold,unit_of_measure,storage_location,supplier,item_status) VALUES(?,?,?,?,?,?,?,?,?,?)",
                r.itemId(),r.name(),r.category(),r.description(),r.availableQuantity(),r.minimumStockThreshold(),r.unitOfMeasure(),r.storageLocation(),r.supplier(),status(r.availableQuantity(),r.minimumStockThreshold()));
        return db.queryForMap("SELECT id,item_id AS itemId,name,category,available_quantity AS availableQuantity,minimum_stock_threshold AS minimumStockThreshold,unit_of_measure AS unitOfMeasure,item_status AS status FROM inventory_items WHERE item_id=?", r.itemId());
    }
    @PostMapping("/inventory/{id}/receive") @Transactional
    public Map<String,Object> receive(@PathVariable long id, @RequestBody QuantityRequest req) { return transact(id, req.quantity(), "RECEIVED"); }
    @PostMapping("/inventory/{id}/issue") @Transactional
    public Map<String,Object> issue(@PathVariable long id, @RequestBody QuantityRequest req) { return transact(id, req.quantity(), "ISSUED"); }
    public record QuantityRequest(@Min(1) int quantity, String notes) {}
    private Map<String,Object> transact(long id, int quantity, String type) {
        if (quantity < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Quantity must be positive");
        var rows = db.query("SELECT available_quantity AS qty, minimum_stock_threshold AS threshold FROM inventory_items WHERE id=? FOR UPDATE", (rs,n)->Map.of("qty",rs.getInt("qty"),"threshold",rs.getInt("threshold")), id);
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Inventory item not found");
        int current=(int)rows.getFirst().get("qty"), threshold=(int)rows.getFirst().get("threshold");
        if(type.equals("ISSUED") && quantity>current) throw new ResponseStatusException(HttpStatus.CONFLICT,"Issue quantity exceeds available stock");
        int updated=type.equals("RECEIVED")?current+quantity:current-quantity;
        db.update("UPDATE inventory_items SET available_quantity=?,item_status=?,last_updated=NOW() WHERE id=?",updated,status(updated,threshold),id);
        db.update("INSERT INTO stock_transactions(inventory_item_id,transaction_type,quantity,notes) VALUES(?,?,?,?)",id,type,quantity,"Web transaction");
        return Map.of("id",id,"availableQuantity",updated,"status",status(updated,threshold));
    }
    private String status(int qty,int threshold){return qty<=threshold?"LOW_STOCK":"AVAILABLE";}

    @GetMapping("/requests") public List<Map<String,Object>> requests(){return db.queryForList("SELECT id,request_id AS requestId,requesting_unit AS requestingUnit,item_name AS itemName,requested_quantity AS requestedQuantity,request_date AS requestDate,priority,reason,status,approved_quantity AS approvedQuantity FROM supply_requests ORDER BY request_date DESC");}
    public record RequestInput(@NotBlank String requestingUnit,@NotBlank String itemName,@Min(1) int requestedQuantity,@NotBlank String priority,String reason){}
    @PostMapping("/requests") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> createRequest(@Valid @RequestBody RequestInput r){String id="REQ-"+UUID.randomUUID().toString().substring(0,8).toUpperCase();db.update("INSERT INTO supply_requests(request_id,requesting_unit,item_name,requested_quantity,priority,reason,status) VALUES(?,?,?,?,?,?, 'Pending')",id,r.requestingUnit(),r.itemName(),r.requestedQuantity(),r.priority(),r.reason());return Map.of("requestId",id,"status","Pending");}
    @PostMapping("/requests/{id}/approve") @Transactional public Map<String,Object> approve(@PathVariable long id,@RequestBody(required=false) Map<String,Integer> body){int qty=body==null?0:body.getOrDefault("approvedQuantity",0);var rows=db.queryForList("SELECT requested_quantity FROM supply_requests WHERE id=? AND status='Pending' FOR UPDATE",id);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.CONFLICT,"Request not found or already processed");int requested=((Number)rows.getFirst().get("requested_quantity")).intValue();if(qty<1)qty=requested;if(qty>requested)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Approved quantity exceeds request");db.update("UPDATE supply_requests SET status=?,approved_quantity=? WHERE id=?",qty==requested?"Approved":"Partially Approved",qty,id);db.update("INSERT INTO request_approvals(supply_request_id,approver,action) VALUES(?,?,?)",id,"authenticated-user","APPROVED");return Map.of("id",id,"status",qty==requested?"Approved":"Partially Approved","approvedQuantity",qty);}
    @PostMapping("/requests/{id}/reject") public Map<String,Object> reject(@PathVariable long id){int n=db.update("UPDATE supply_requests SET status='Rejected' WHERE id=? AND status='Pending'",id);if(n==0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Request not found or already processed");db.update("INSERT INTO request_approvals(supply_request_id,approver,action) VALUES(?,?,?)",id,"authenticated-user","REJECTED");return Map.of("id",id,"status","Rejected");}
    @GetMapping("/notifications") public List<Map<String,Object>> notifications(){return db.queryForList("SELECT id,title,message,category,is_read AS isRead,created_at AS createdAt FROM notifications ORDER BY created_at DESC LIMIT 50");}
    @PutMapping("/notifications/{id}/read") public Map<String,Object> markRead(@PathVariable long id){db.update("UPDATE notifications SET is_read=TRUE WHERE id=?",id);return Map.of("id",id,"read",true);}
}
