package com.example.demo.service;

import com.example.demo.dto.CreateGroupRequest;
import com.example.demo.dto.GroupResponse;
import com.example.demo.dto.MemberResponse;
import com.example.demo.entity.Group;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupService {
    private final GroupRepository groupRepository;

    public GroupResponse createGroup(CreateGroupRequest request) {
        Group group = new Group();
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        
        Group savedGroup = groupRepository.save(group);
        return mapToResponse(savedGroup);
    }

    public java.util.List<GroupResponse> getAllGroups() {
        return groupRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public GroupResponse getGroup(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        return mapToResponse(group);
    }

    public GroupResponse updateGroup(Long groupId, CreateGroupRequest request) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        
        Group updatedGroup = groupRepository.save(group);
        return mapToResponse(updatedGroup);
    }

    public void deleteGroup(Long groupId) {
        if (!groupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException("Group not found");
        }
        groupRepository.deleteById(groupId);
    }

    private GroupResponse mapToResponse(Group group) {
        GroupResponse response = new GroupResponse();
        response.setId(group.getId());
        response.setName(group.getName());
        response.setDescription(group.getDescription());
        response.setCreatedAt(group.getCreatedAt());
        
        if (group.getMembers() != null) {
            response.setMembers(group.getMembers().stream()
                    .map(member -> new MemberResponse(member.getId(), member.getName(), member.getGroupId()))
                    .collect(Collectors.toList()));
        }
        
        return response;
    }
}
