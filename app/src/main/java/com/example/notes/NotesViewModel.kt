package com.example.notes

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

data class NoteViewModel(

    @JsonProperty("id")
    var Id : String = "",

    @JsonProperty("text")
    var Text : String = "",

    @JsonProperty("title")
    var Title : String,

    @JsonProperty("createdAt")
    var CreatedAt : String,

    @JsonProperty("updatedAt")
    var UpdatedAt : String

) : Serializable
