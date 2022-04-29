package tech.phegy.api.controller;

import org.springframework.web.bind.annotation.*;
import tech.phegy.api.dto.poins.request.VoteDto;
import tech.phegy.api.dto.poins.response.PointsBagResponseDto;
import tech.phegy.api.mapper.poins.PoinsMapper;
import tech.phegy.api.service.points.PointsBagService;
import tech.phegy.api.service.points.VoteService;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/points")
public class PointsController {
    private final VoteService voteService;
    private final PointsBagService pointsBagService;
    private final PoinsMapper poinsMapper;

    public PointsController(VoteService voteService,
                            PointsBagService pointsBagService,
                            PoinsMapper poinsMapper) {
        this.voteService = voteService;
        this.pointsBagService = pointsBagService;
        this.poinsMapper = poinsMapper;
    }

    @GetMapping("/bag")
    public PointsBagResponseDto getPoinsBag(Principal principal) {
        return poinsMapper.pointsBagToPointsBagResponseDto(
                pointsBagService.getPointsBag(principal.getName()));
    }

    @PostMapping("/vote")
    public void vote(@RequestBody VoteDto voteDto, Principal principal) {
        this.voteService.vote(voteDto.getImageId(), Math.round(voteDto.getPoints() * 10.0) / 10.0, principal.getName());
    }
}
