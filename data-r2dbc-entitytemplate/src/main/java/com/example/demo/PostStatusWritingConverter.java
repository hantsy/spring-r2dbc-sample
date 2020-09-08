package com.example.demo;

import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.convert.EnumWriteSupport;

@WritingConverter
public class PostStatusWritingConverter extends EnumWriteSupport<Post.Status> {
}
