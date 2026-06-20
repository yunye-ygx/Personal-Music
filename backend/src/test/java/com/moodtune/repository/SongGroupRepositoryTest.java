package com.moodtune.repository;

import com.moodtune.entity.SongGroup;
import com.moodtune.entity.SongGroupRelation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class SongGroupRepositoryTest {

    @Autowired
    private SongGroupRepository groupRepository;

    @Autowired
    private SongGroupRelationRepository relationRepository;

    @Test
    void createGroupAndRelations() {
        SongGroup group = new SongGroup();
        group.setName("睡前听的歌");
        group.setSource("manual");
        SongGroup savedGroup = groupRepository.save(group);

        SongGroupRelation rel = new SongGroupRelation();
        rel.setSongId(1L);
        rel.setGroupId(savedGroup.getId());
        relationRepository.save(rel);

        List<SongGroupRelation> relations = relationRepository.findByGroupId(savedGroup.getId());
        assertEquals(1, relations.size());
    }

    @Test
    void findGroupsBySource() {
        SongGroup g1 = new SongGroup();
        g1.setName("导入歌单");
        g1.setSource("imported");
        groupRepository.save(g1);

        SongGroup g2 = new SongGroup();
        g2.setName("手动分组");
        g2.setSource("manual");
        groupRepository.save(g2);

        List<SongGroup> imported = groupRepository.findBySource("imported");
        assertEquals(1, imported.size());
        assertEquals("导入歌单", imported.get(0).getName());
    }
}
