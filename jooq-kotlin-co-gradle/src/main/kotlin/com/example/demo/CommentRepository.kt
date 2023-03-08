package com.example.demo

import org.springframework.data.repository.kotlin.CoroutineSortingRepository
import java.util.*

interface CommentRepository : CoroutineSortingRepository<Comment, UUID>