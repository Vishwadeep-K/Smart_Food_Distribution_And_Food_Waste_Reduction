package com.annam.service;

import com.annam.graph.DijkstraAlgorithm;
import com.annam.graph.Location;
import com.annam.graph.PathResult;
import com.annam.graph.RoadNetwork;
import com.annam.repository.LocationRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
public class RoadNetworkService {
    private final LocationRepository locationRepository;
    private RoadNetwork roadNetwork;

    public RoadNetworkService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
        reloadNetwork();
    }

    public synchronized void reloadNetwork() {
        this.roadNetwork = locationRepository.loadRoadNetwork();
    }

    public synchronized RoadNetwork getRoadNetwork() {
        return roadNetwork;
    }

    public Location getLocation(String id) {
        return roadNetwork.getLocation(id);
    }

    public Collection<Location> getAllLocations() {
        return roadNetwork.getAllLocations();
    }

    public List<LocationRepository.RoadDto> getAllRoads() {
        return locationRepository.findAllRoads();
    }

    public PathResult findShortestRoute(String sourceId, String destId) {
        Location src = getLocation(sourceId);
        Location dst = getLocation(destId);
        if (src == null || dst == null) {
            return PathResult.unreachable(src, dst);
        }
        return DijkstraAlgorithm.findShortestPath(roadNetwork, src, dst);
    }
}
