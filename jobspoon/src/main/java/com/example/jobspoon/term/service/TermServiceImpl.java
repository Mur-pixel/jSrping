package com.example.jobspoon.term.service;


import com.example.jobspoon.term.entity.Category;
import com.example.jobspoon.term.entity.Tag;
import com.example.jobspoon.term.entity.Term;
import com.example.jobspoon.term.entity.TermTag;
import com.example.jobspoon.term.repository.CategoryRepository;
import com.example.jobspoon.term.repository.TagRepository;
import com.example.jobspoon.term.repository.TermRepository;
import com.example.jobspoon.term.repository.TermTagRepository;
import com.example.jobspoon.term.service.request.CreateTermRequest;
import com.example.jobspoon.term.service.response.CreateTermResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TermServiceImpl implements TermService {

    private final TermRepository termRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final TermTagRepository termTagRepository;

    @Override
    public CreateTermResponse register(CreateTermRequest createTermRequest) {

        // 카테고리 조회
        // 처음 테스트를 수행하는 경우 카테고리가 등록되어 있지 않아 Null 값 발생
        // aim_db > category 테이블에 다음과 같은 예시 데이터를 입력 후 테스트 진행 필요
        // INSERT INTO category (id, type, group_name, name, depth, sort_order) VALUES ('CAT001', '직무 중심', 'Backend', 'Java', 2, 1);
        // |---------> mysql에서 데이터 넣는 것 대신 DataInitializer.java 에서 데이터 넣을 수 있도록 해둠
        Category category = categoryRepository.findById(createTermRequest.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));

        if (category.getDepth() != 2) {
            throw new IllegalArgumentException("용어 등록 시 소분류만 선택할 수 있습니다.");
        }

        // Term 생성 및 저장
        Term term = createTermRequest.toTerm(category);
        Term savedTerm = termRepository.save(term);

        // 중복 제거 및 정렬
        List<String> tagNames = parseTags(createTermRequest.getTags())
                .stream()
                .distinct()
                .toList();

        // 태그 저장 및 TermTag 연결
        List<String> savedTagNames = tagNames.stream().map(tagName -> {
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> tagRepository.save(new Tag(null, tagName)));

            termTagRepository.save(new TermTag(savedTerm, tag));
            return tag.getName();
        }).toList();

        return CreateTermResponse.from(savedTerm, savedTagNames, category);
    }

    private List<String> parseTags(String rawTagString) {
        if (rawTagString == null || rawTagString.isBlank()) {
            return List.of();
        }

        return Arrays.stream(rawTagString.trim().split("#"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }
}
