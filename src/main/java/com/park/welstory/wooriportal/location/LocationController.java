package com.park.welstory.wooriportal.location;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RequiredArgsConstructor
@Controller
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/location/list")
    @ResponseBody
    public List<LocationDTO> locationList(String type, Long buildingNum) {
        return locationService.getListLocation(type, buildingNum);
    }

    @PostMapping(value = "/location/add")
    @ResponseBody
    public ResponseEntity<String> locationAdd(LocationDTO locationDTO,
                                              @RequestParam(required = false) String imageDelete) {
        locationService.locationAdd(locationDTO, imageDelete);
        return ResponseEntity.ok("success");
    }

    @PostMapping("/location/delete")
    @ResponseBody
    public ResponseEntity<String> locationDelete(LocationDTO locationDTO) {
        locationService.locationDelete(locationDTO);
        return ResponseEntity.ok("success");
    }
}