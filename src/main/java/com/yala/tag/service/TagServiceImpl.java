package com.yala.tag.service;

import com.yala.tag.model.Tag;
import com.yala.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;

    @Override
    @Transactional
    public List<Tag> findOrCreateTags(List<String> names) {
        if (names == null || names.isEmpty()) return new ArrayList<>();
        List<Tag> result = new ArrayList<>();
        for (String name : names) {
            Tag tag = tagRepository.findByName(name)
                    .orElseGet(() -> tagRepository.save(Tag.builder().name(name).build()));
            result.add(tag);
        }
        return result;
    }
}
