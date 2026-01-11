package com.example.demo.domain.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table(value = "nodes")
data class Node(
    @Id
    @Column("id")
    val id: UUID? = null,

    @Column("name")
    var name: String? = null,

    @Column("description")
    var description: String? = null,

    @Column("parent_id")
    val parentId: UUID? = null
)