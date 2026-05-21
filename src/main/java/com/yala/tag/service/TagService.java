package com.yala.tag.service;

import com.yala.tag.model.Tag;

import java.util.List;

public interface TagService {
    List<Tag> findOrCreateTags(List<String> names);
}
