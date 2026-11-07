package com.rakshaklogix;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DemoSeeder implements CommandLineRunner {
    private final JdbcTemplate db;
    private final PasswordEncoder encoder;
    public DemoSeeder(JdbcTemplate db, PasswordEncoder encoder) { this.db = db; this.encoder = encoder; }
    @Override public void run(String... args) {
        if (db.queryForObject("SELECT COUNT(*) FROM users", Long.class) == 0) {
            db.update("INSERT INTO units(code,name,location) VALUES('UNIT-DEMO','Demo Logistics Unit','Synthetic Demo Region')");
            long unit = db.queryForObject("SELECT id FROM units WHERE code='UNIT-DEMO'", Long.class);
            seedUser("admin@rakshaklogix.local","Admin@123","Rakshak Administrator","ADMIN",unit);
            seedUser("logistics@rakshaklogix.local","Logistics@123","Logistics Officer","LOGISTICS_OFFICER",unit);
            seedUser("maintenance@rakshaklogix.local","Maintenance@123","Maintenance Officer","MAINTENANCE_OFFICER",unit);
            seedUser("unit@rakshaklogix.local","Unit@123","Unit Officer","UNIT_OFFICER",unit);
        }
        if (db.queryForObject("SELECT COUNT(*) FROM inventory_items", Long.class) == 0) {
            db.update("INSERT INTO inventory_items(item_id,name,category,description,available_quantity,minimum_stock_threshold,unit_of_measure,storage_location,supplier,item_status) VALUES ('MED-001','First Aid Kits','Medical Supplies','Synthetic training inventory',240,60,'kits','Central Depot A','Demo Supplier','AVAILABLE'),('FUEL-001','Diesel Reserve','Fuel','Synthetic training inventory',1800,500,'litres','Fuel Depot B','Demo Supplier','AVAILABLE'),('SP-001','Generator Filters','Spare Parts','Synthetic training inventory',24,30,'units','Workshop Store','Demo Supplier','LOW_STOCK'),('FOOD-001','Ration Packs','Food Supplies','Synthetic training inventory',720,200,'packs','Central Depot A','Demo Supplier','AVAILABLE')");
            db.update("INSERT INTO equipment(equipment_id,name,category,serial_number,assigned_unit,current_condition,service_status) VALUES ('EQ-001','Portable Generator','Power Equipment','SYN-EQ-001','Demo Logistics Unit','Good','Available'),('EQ-002','Field Radio Set','Communication Equipment','SYN-EQ-002','Demo Logistics Unit','Fair','Under Maintenance')");
            db.update("INSERT INTO supply_requests(request_id,requesting_unit,item_name,requested_quantity,priority,reason,status) VALUES ('REQ-DEMO-001','Demo Logistics Unit','Generator Filters',12,'High','Synthetic demo replenishment request','Pending')");
            db.update("INSERT INTO notifications(title,message,category) VALUES ('Low stock alert','Generator Filters are below the configured threshold.','INVENTORY'),('Demo environment ready','Synthetic sample data has been loaded.','SYSTEM')");
            db.update("INSERT INTO consumption_history(item_name,category,consumption_date,quantity_consumed) VALUES ('First Aid Kits','Medical Supplies',DATE_SUB(NOW(),INTERVAL 30 DAY),21),('First Aid Kits','Medical Supplies',DATE_SUB(NOW(),INTERVAL 20 DAY),24),('First Aid Kits','Medical Supplies',DATE_SUB(NOW(),INTERVAL 10 DAY),28),('Generator Filters','Spare Parts',DATE_SUB(NOW(),INTERVAL 30 DAY),8),('Generator Filters','Spare Parts',DATE_SUB(NOW(),INTERVAL 20 DAY),10),('Generator Filters','Spare Parts',DATE_SUB(NOW(),INTERVAL 10 DAY),12)");
        }
    }
    private void seedUser(String email,String password,String name,String role,long unit){db.update("INSERT INTO users(username,email,password,full_name,role,unit_id) VALUES(?,?,?,?,?,?)",email,email,encoder.encode(password),name,role,unit);}
}
