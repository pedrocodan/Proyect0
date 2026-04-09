package com.example.demo.service;

import com.example.demo.dto.CreateMemberRequest;
import com.example.demo.dto.MemberResponse;
import com.example.demo.entity.Group;
import com.example.demo.entity.Member;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.GroupRepository;
import com.example.demo.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {
    private final MemberRepository memberRepository;
    private final GroupRepository groupRepository;

    public MemberResponse addMember(Long groupId, CreateMemberRequest request) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        
        Member member = new Member();
        member.setName(request.getName());
        member.setGroup(group);
        
        Member savedMember = memberRepository.save(member);
        return mapToResponse(savedMember);
    }

    public List<MemberResponse> getGroupMembers(Long groupId) {
        if (!groupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException("Group not found");
        }
        
        return memberRepository.findByGroupId(groupId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public MemberResponse getMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        return mapToResponse(member);
    }

    public void removeMember(Long groupId, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        
        if (!member.getGroupId().equals(groupId)) {
            throw new ResourceNotFoundException("Member does not belong to this group");
        }
        
        memberRepository.deleteById(memberId);
    }

    private MemberResponse mapToResponse(Member member) {
        return new MemberResponse(member.getId(), member.getName(), member.getGroupId());
    }
}
