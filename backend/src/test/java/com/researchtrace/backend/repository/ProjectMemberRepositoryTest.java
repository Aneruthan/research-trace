package com.researchtrace.backend.repository;

import com.researchtrace.backend.model.ProjectMember;
import com.researchtrace.backend.model.ProjectMemberRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProjectMemberRepositoryTest {

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findsMembersOfOneProjectOnly() {
        projectMemberRepository.save(member(1L, 7L, ProjectMemberRole.STUDENT));
        projectMemberRepository.save(member(1L, 8L, ProjectMemberRole.STUDENT));
        projectMemberRepository.save(member(1L, 3L, ProjectMemberRole.PROFESSOR));
        projectMemberRepository.save(member(2L, 9L, ProjectMemberRole.STUDENT));
        entityManager.flush();
        entityManager.clear();

        List<ProjectMember> members = projectMemberRepository.findByProjectId(1L);

        assertThat(members)
            .extracting(ProjectMember::getUserId)
            .containsExactlyInAnyOrder(7L, 8L, 3L);
    }

    @Test
    void returnsEmptyListForProjectWithoutMembers() {
        projectMemberRepository.save(member(1L, 7L, ProjectMemberRole.STUDENT));
        entityManager.flush();
        entityManager.clear();

        assertThat(projectMemberRepository.findByProjectId(5L)).isEmpty();
    }

    private ProjectMember member(Long projectId, Long userId, ProjectMemberRole role) {
        ProjectMember member = new ProjectMember();
        member.setProjectId(projectId);
        member.setUserId(userId);
        member.setRole(role);
        return member;
    }
}
