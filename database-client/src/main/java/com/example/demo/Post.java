package com.example.demo;

import lombok.*;

/**
 * @author hantsy
 */
@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
class Post {

    private Integer id;

    private String title;

    private String content;

}
