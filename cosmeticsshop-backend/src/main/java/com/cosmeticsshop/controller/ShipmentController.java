package com.cosmeticsshop.controller;

import com.cosmeticsshop.model.Shipment;
import com.cosmeticsshop.service.ShipmentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CORPORATE')")
    public List<Shipment> getShipments() {
        return shipmentService.getAllShipments();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CORPORATE')")
    public Shipment createShipment(@RequestBody Shipment shipment) {
        return shipmentService.save(shipment);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CORPORATE')")
    public Shipment updateShipment(@PathVariable Long id, @RequestBody Shipment shipment) {
        shipment.setId(id);
        return shipmentService.save(shipment);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CORPORATE')")
    public void deleteShipment(@PathVariable Long id) {
        shipmentService.deleteShipment(id);
    }

    @GetMapping("/order/{orderId}")
    public Shipment getShipmentByOrder(@PathVariable Long orderId) {
        return shipmentService.getByOrderId(orderId);
    }
}
