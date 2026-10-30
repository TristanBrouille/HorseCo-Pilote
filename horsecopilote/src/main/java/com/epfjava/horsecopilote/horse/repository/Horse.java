package com.epfjava.horsecopilote.horse.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "horse")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Horse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String breed;

    @Column(nullable = false)
    private int heightCm;

    @Column(nullable = false)
    private int weightKg;

    @Column(nullable = false)
    private int dailyHours;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private HorseStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Location location;

    @Column(nullable = false)
    private int requiredLevel;

    public Horse(String name, String breed, int heightCm, int weightKg, int dailyHours, HorseStatus status,
                 Location location, int requiredLevel) {
        this.name = name;
        this.breed = breed;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.dailyHours = dailyHours;
        this.status = status;
        this.location = location;
        this.requiredLevel = requiredLevel;
    }

    public void update(Horse changes) {
        this.name = changes.name;
        this.breed = changes.breed;
        this.heightCm = changes.heightCm;
        this.weightKg = changes.weightKg;
        this.dailyHours = changes.dailyHours;
        this.status = changes.status;
        this.location = changes.location;
        this.requiredLevel = changes.requiredLevel;
    }
}
