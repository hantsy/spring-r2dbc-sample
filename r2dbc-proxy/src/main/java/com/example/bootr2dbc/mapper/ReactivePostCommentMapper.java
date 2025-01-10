package com.example.bootr2dbc.mapper;

import com.example.bootr2dbc.entities.ReactiveComments;
import com.example.bootr2dbc.model.ReactiveCommentRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ReactivePostCommentMapper {

    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    ReactiveComments mapToReactivePostComments(ReactiveCommentRequest reactiveCommentRequest);

    void updateReactiveCommentRequestFromReactiveComments(
            ReactiveCommentRequest reactiveCommentRequest,
            @MappingTarget ReactiveComments reactiveComments);
}
