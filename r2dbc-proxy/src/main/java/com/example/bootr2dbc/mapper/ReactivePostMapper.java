package com.example.bootr2dbc.mapper;

import com.example.bootr2dbc.entities.ReactivePost;
import com.example.bootr2dbc.model.ReactivePostRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ReactivePostMapper {

    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    ReactivePost mapToReactivePost(ReactivePostRequest reactivePostRequest);

    void updateReactivePostFromReactivePostRequest(
            ReactivePostRequest reactivePostRequest, @MappingTarget ReactivePost reactivePost);
}
