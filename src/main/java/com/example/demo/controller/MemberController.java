package com.example.demo.controller;

import com.example.demo.dto.CreateMemberRequest;
import com.example.demo.dto.MemberResponse;
import com.example.demo.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/grupos/{groupId}/miembros")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MemberController {
    private final MemberService memberService;

    @PostMapping
    public ResponseEntity<MemberResponse> addMember(
            @PathVariable Long groupId,
            @RequestBody CreateMemberRequest request) {
        MemberResponse response = memberService.addMember(groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<MemberResponse>> getGroupMembers(@PathVariable Long groupId) {
        List<MemberResponse> members = memberService.getGroupMembers(groupId);
        return ResponseEntity.ok(members);
    }

    @GetMapping("/{memberId}")
    public ResponseEntity<MemberResponse> getMember(@PathVariable Long memberId) {
        MemberResponse response = memberService.getMember(memberId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long groupId,
            @PathVariable Long memberId) {
        memberService.removeMember(groupId, memberId);
        return ResponseEntity.noContent().build();
    }
}
