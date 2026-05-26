package com.gepardec.zep.model;

import jakarta.json.bind.annotation.JsonbProperty;

import java.util.Objects;

/**
 * Subtask - Manually created to satisfy OpenAPI generation gaps
 */
public class Subtask {

    @JsonbProperty("id")
    private Integer id;

    @JsonbProperty("name")
    private String name;

    @JsonbProperty("description")
    private String description;

    public Subtask id(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Subtask name(String name) {
        this.name = name;
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Subtask description(String description) {
        this.description = description;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Subtask subtask = (Subtask) o;
        return Objects.equals(this.id, subtask.id) &&
                Objects.equals(this.name, subtask.name) &&
                Objects.equals(this.description, subtask.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description);
    }

    @Override
    public String toString() {
        return "class Subtask {\n" +
                "    id: " + toIndentedString(id) + "\n" +
                "    name: " + toIndentedString(name) + "\n" +
                "    description: " + toIndentedString(description) + "\n" +
                "}";
    }

    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
