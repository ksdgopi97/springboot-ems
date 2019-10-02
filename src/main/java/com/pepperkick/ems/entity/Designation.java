package com.pepperkick.ems.entity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "DESIGNATION")
public class Designation implements Comparable<Designation> {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "TITLE", unique = true)
    private String title;

    @Column(name = "LEVEL")
    private float level;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public float getLevel() {
        return level;
    }

    public void setLevel(float level) {
        this.level = level;
    }

    @Override
    public int compareTo(Designation o) {
        if (this.getLevel() == o.getLevel() && this.getTitle().compareTo(o.getTitle()) == 0)
            return 0;

        return -1;
    }
}
