package com.annam.controller;

import com.annam.graph.Location;
import com.annam.graph.PathResult;
import com.annam.repository.LocationRepository;
import com.annam.service.RoadNetworkService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/routing")
public class RoutingController {
    private final RoadNetworkService roadNetworkService;

    public RoutingController(RoadNetworkService roadNetworkService) {
        this.roadNetworkService = roadNetworkService;
    }

    @GetMapping("/locations")
    public ResponseEntity<Collection<Location>> getAllLocations() {
        return ResponseEntity.ok(roadNetworkService.getAllLocations());
    }

    @GetMapping("/roads")
    public ResponseEntity<List<LocationRepository.RoadDto>> getAllRoads() {
        return ResponseEntity.ok(roadNetworkService.getAllRoads());
    }

    @GetMapping("/shortest-path")
    public ResponseEntity<?> getShortestPath(
            @RequestParam String sourceId,
            @RequestParam String destId
    ) {
        PathResult res = roadNetworkService.findShortestRoute(sourceId, destId);
        return ResponseEntity.ok(Map.of(
                "sourceId", sourceId.toUpperCase(),
                "destId", destId.toUpperCase(),
                "reachable", res.isReachable(),
                "travelTimeMinutes", res.getTotalTravelTimeMinutes(),
                "formattedRoute", res.getFormattedRoute(),
                "pathNodes", res.getPath().stream().map(Location::getName).toList()
        ));
    }
}
