package com.sumit.spring_boot_crash_course.controller

import com.sumit.spring_boot_crash_course.database.model.Note
import com.sumit.spring_boot_crash_course.database.repository.NoteRepository
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.bson.types.ObjectId
import org.springframework.data.domain.PageRequest
import org.springframework.data.web.PagedModel
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/notes")
class NoteController (
    private val repository: NoteRepository
) {

    data class NoteRequest(
        val id: String?,
        @field:NotBlank(message = "Title cannot be blank.")
        val title: String,
        val content: String,
        val color:Long
    )

    data class NoteResponse(
        val id: String,
        val title: String,
        val content: String,
        val color:Long,
        val createdAt: Instant
    )

    @PostMapping
    fun save(
        @Valid @RequestBody body: NoteRequest): NoteResponse {
        val ownerId = SecurityContextHolder.getContext().authentication.principal as String
        val note = repository.save(
            Note(
                id = body.id?.let { ObjectId(it) } ?: ObjectId.get(),
                title = body.title,
                content = body.content,
                color = body.color,
                createdAt = Instant.now(),
                ownerId = ObjectId(ownerId)
            )
        )
        return note.toResponse()
    }

    @GetMapping
    fun findByOwnerId(
        ownerId: String = SecurityContextHolder.getContext().authentication.principal as String,
        @RequestParam(value = "page", defaultValue = "0") page: Int = 0,
        @RequestParam(value = "size", defaultValue = "10") size: Int = 0,
    ): PagedModel<NoteResponse>{
        val pageable = PageRequest.of(page, size)
        val pageResult = repository.findByOwnerId(ObjectId(ownerId), pageable = pageable).map {
            it.toResponse()
        }
        return PagedModel(pageResult)
    }

    @DeleteMapping(path = ["/{id}"])
    fun deleteById(@PathVariable id: String){
        val note = repository.findById(ObjectId(id))
            .orElseThrow {
                IllegalArgumentException("Note not found")
            }
        val ownerId = SecurityContextHolder.getContext().authentication.principal as String
        if (note.ownerId.toHexString() == ownerId){
            repository.deleteById(ObjectId(id))
        }
    }


    private fun Note.toResponse(): NoteController.NoteResponse {
        return NoteResponse(
            id = id.toHexString(),
            title = title,
            content = content,
            color = color,
            createdAt = createdAt
        )
    }
}