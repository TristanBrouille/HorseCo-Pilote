package com.epfjava.horsecopilote.lesson.service;

import com.epfjava.horsecopilote.horse.repository.Horse;
import com.epfjava.horsecopilote.horse.repository.HorseStatus;
import com.epfjava.horsecopilote.lesson.presentation.HorseUnavailabilityReason;
import com.epfjava.horsecopilote.lesson.repository.Lesson;
import com.epfjava.horsecopilote.lesson.repository.LessonBooking;
import com.epfjava.horsecopilote.lesson.repository.LessonStatus;
import com.epfjava.horsecopilote.security.repository.Roles;
import com.epfjava.horsecopilote.security.repository.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.epfjava.horsecopilote.lesson.service.LessonFixtures.WEDNESDAY_17H;
import static com.epfjava.horsecopilote.lesson.service.LessonFixtures.availableHorse;
import static com.epfjava.horsecopilote.lesson.service.LessonFixtures.booking;
import static com.epfjava.horsecopilote.lesson.service.LessonFixtures.horse;
import static com.epfjava.horsecopilote.lesson.service.LessonFixtures.lesson;
import static com.epfjava.horsecopilote.lesson.service.LessonFixtures.member;
import static org.assertj.core.api.Assertions.assertThat;

class HorseEligibilityTest {

    private final User coach = new User(4L, Roles.COACH, "Martin", "Sophie", "sophie@example.com", "secret");
    private final Lesson lesson = lesson(10L, coach, WEDNESDAY_17H, LessonStatus.PREVUE);
    private final LessonBooking target = booking(100L, lesson, member(9L, 4, "60.00"), null);

    @Test
    void anAvailableCompatibleHorseIsSelectable() {
        assertThat(HorseEligibility.check(availableHorse(1L, "Tornado"), target, List.of())).isEmpty();
    }

    @Test
    void aHorseThatIsNotAvailableIsRefused() {
        Horse resting = horse(1L, "Ulysse", 540, 3, HorseStatus.REPOS, 1);

        assertThat(HorseEligibility.check(resting, target, List.of()))
                .contains(HorseUnavailabilityReason.STATUS);
    }

    @Test
    void aHorseRequiringAHigherLevelIsRefused() {
        Horse demanding = horse(1L, "Rubis", 620, 3, HorseStatus.DISPONIBLE, 5);

        assertThat(HorseEligibility.check(demanding, target, List.of()))
                .contains(HorseUnavailabilityReason.LEVEL);
    }

    @Test
    void aHorseRequiringExactlyTheRiderLevelIsAccepted() {
        Horse matching = horse(1L, "Éclair", 450, 3, HorseStatus.DISPONIBLE, 4);

        assertThat(HorseEligibility.check(matching, target, List.of())).isEmpty();
    }

    @Test
    void aRiderHeavierThanTwentyPercentOfTheHorseIsRefused() {
        Horse light = horse(1L, "Vanille", 299, 3, HorseStatus.DISPONIBLE, 1);

        assertThat(HorseEligibility.check(light, target, List.of()))
                .contains(HorseUnavailabilityReason.WEIGHT);
    }

    @Test
    void aRiderWeighingExactlyTwentyPercentOfTheHorseIsAccepted() {
        Horse justEnough = horse(1L, "Vanille", 300, 3, HorseStatus.DISPONIBLE, 1);

        assertThat(HorseEligibility.check(justEnough, target, List.of())).isEmpty();
    }

    @Test
    void aHorseAlreadyRiddenByAnotherParticipantOfTheLessonIsRefused() {
        Horse tornado = availableHorse(1L, "Tornado");
        LessonBooking other = booking(101L, lesson, member(10L, 5, "70.00"), tornado);

        assertThat(HorseEligibility.check(tornado, target, List.of(other)))
                .contains(HorseUnavailabilityReason.ALREADY_ASSIGNED);
    }

    @Test
    void aHorseAssignedToASimultaneousLessonIsRefused() {
        Horse tornado = availableHorse(1L, "Tornado");
        Lesson simultaneous = lesson(11L, coach, WEDNESDAY_17H, LessonStatus.PREVUE);

        assertThat(HorseEligibility.check(tornado, target,
                List.of(booking(200L, simultaneous, member(5L, 3, "50.00"), tornado))))
                .contains(HorseUnavailabilityReason.ALREADY_ASSIGNED);
    }

    @Test
    void theHorseAlreadyAssignedToTheParticipantStaysSelectable() {
        Horse tornado = availableHorse(1L, "Tornado");
        LessonBooking assigned = booking(100L, lesson, target.getMember(), tornado);

        assertThat(HorseEligibility.check(tornado, assigned, List.of(assigned))).isEmpty();
    }

    @Test
    void aHorseThatReachedItsDailyHoursIsRefused() {
        Horse twoHoursADay = horse(1L, "Rubis", 620, 2, HorseStatus.DISPONIBLE, 1);
        List<LessonBooking> day = List.of(
                booking(201L, lesson(11L, coach, WEDNESDAY_17H.minusHours(6), LessonStatus.PREVUE),
                        member(5L, 3, "50.00"), twoHoursADay),
                booking(202L, lesson(12L, coach, WEDNESDAY_17H.minusHours(3), LessonStatus.TERMINEE),
                        member(6L, 3, "50.00"), twoHoursADay));

        assertThat(HorseEligibility.check(twoHoursADay, target, day))
                .contains(HorseUnavailabilityReason.DAILY_HOURS);
    }

    @Test
    void aHorseWorkingTheTwoPreviousHoursIsRefused() {
        Horse tornado = availableHorse(1L, "Tornado");
        List<LessonBooking> day = List.of(
                booking(201L, lesson(11L, coach, WEDNESDAY_17H.minusHours(2), LessonStatus.PREVUE),
                        member(5L, 3, "50.00"), tornado),
                booking(202L, lesson(12L, coach, WEDNESDAY_17H.minusHours(1), LessonStatus.PREVUE),
                        member(6L, 3, "50.00"), tornado));

        assertThat(HorseEligibility.check(tornado, target, day))
                .contains(HorseUnavailabilityReason.CONSECUTIVE_HOURS);
    }

    @Test
    void aHorseWorkingTheHourBeforeAndTheHourAfterIsRefused() {
        Horse tornado = availableHorse(1L, "Tornado");
        List<LessonBooking> day = List.of(
                booking(201L, lesson(11L, coach, WEDNESDAY_17H.minusHours(1), LessonStatus.PREVUE),
                        member(5L, 3, "50.00"), tornado),
                booking(202L, lesson(12L, coach, WEDNESDAY_17H.plusHours(1), LessonStatus.PREVUE),
                        member(6L, 3, "50.00"), tornado));

        assertThat(HorseEligibility.check(tornado, target, day))
                .contains(HorseUnavailabilityReason.CONSECUTIVE_HOURS);
    }

    @Test
    void aHorseWithABreakBeforeTheLessonIsAccepted() {
        Horse tornado = availableHorse(1L, "Tornado");
        List<LessonBooking> day = List.of(
                booking(201L, lesson(11L, coach, WEDNESDAY_17H.minusHours(3), LessonStatus.PREVUE),
                        member(5L, 3, "50.00"), tornado),
                booking(202L, lesson(12L, coach, WEDNESDAY_17H.minusHours(1), LessonStatus.PREVUE),
                        member(6L, 3, "50.00"), tornado));

        assertThat(HorseEligibility.check(tornado, target, day)).isEmpty();
    }

    @Test
    void hoursOfDayCountsEachLessonOfTheHorseOnce() {
        Horse tornado = availableHorse(1L, "Tornado");
        Lesson morning = lesson(11L, coach, WEDNESDAY_17H.minusHours(6), LessonStatus.PREVUE);
        List<LessonBooking> day = List.of(
                booking(201L, morning, member(5L, 3, "50.00"), tornado),
                booking(202L, lesson, member(6L, 3, "50.00"), tornado),
                booking(203L, lesson, member(7L, 3, "50.00"), availableHorse(2L, "Caramel")));

        assertThat(HorseEligibility.hoursOfDay(tornado, day)).isEqualTo(2);
    }
}
