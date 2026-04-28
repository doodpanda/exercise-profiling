package com.advpro.profiling.tutorial.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.advpro.profiling.tutorial.model.Course;
import com.advpro.profiling.tutorial.model.Student;
import com.advpro.profiling.tutorial.model.StudentCourse;
import com.advpro.profiling.tutorial.repository.CourseRepository;
import com.advpro.profiling.tutorial.repository.StudentCourseRepository;
import com.advpro.profiling.tutorial.repository.StudentRepository;
import com.github.javafaker.Faker;

import jakarta.transaction.Transactional;

/**
 * @author muhammad.khadafi
 */
@Service
public class DataSeedService {

    // optimized the seeding process hehe
    private static final int NUMBER_OF_STUDENTS = 20_000;
    private static final int NUMBER_OF_COURSE = 10;
    private static final int COURSES_PER_STUDENT = 2;
    private static final int BATCH_SIZE = 1_000;
    private static final Locale INDONESIAN_LOCALE = new Locale("in-ID");

    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private StudentCourseRepository studentCourseRepository;

    @Transactional
    public void seedStudent() {
        Faker faker = new Faker(INDONESIAN_LOCALE);
        List<Student> students = new ArrayList<>(BATCH_SIZE);

        for (int i = 0; i < NUMBER_OF_STUDENTS; i++) {
            Student student = new Student();
            student.setStudentCode(faker.code().ean8());
            student.setName(faker.name().fullName());
            student.setFaculty(faker.educator().course());
            student.setGpa(faker.number().randomDouble(2, 2, 4));

            students.add(student);
            flushBatch(students, studentRepository);
        }

        flushRemaining(students, studentRepository);
    }

    @Transactional
    public void seedCourse() {
        Faker faker = new Faker(INDONESIAN_LOCALE);
        List<Course> courses = new ArrayList<>(NUMBER_OF_COURSE);

        for (int i = 0; i < NUMBER_OF_COURSE; i++) {
            Course course = new Course();
            course.setCourseCode(faker.code().ean8());
            course.setName(faker.book().title());
            course.setDescription(faker.lorem().sentence());

            courses.add(course);
        }

        courseRepository.saveAll(courses);
    }

    @Transactional
    public void seedStudentCourses() {
        List<Student> students = studentRepository.findAll();
        List<Course> courses = courseRepository.findAll();
        Random random = new Random();
        List<StudentCourse> studentCourses = new ArrayList<>(BATCH_SIZE);

        for (Student student : students) {
            Set<Integer> selectedCourseIndexes = selectCourseIndexes(random, courses.size());

            for (Integer selectedCourseIndex : selectedCourseIndexes) {
                Course course = courses.get(selectedCourseIndex);
                StudentCourse studentCourse = new StudentCourse(student, course);
                studentCourses.add(studentCourse);
                flushBatch(studentCourses, studentCourseRepository);
            }
        }
        flushRemaining(studentCourses, studentCourseRepository);
    }

    private Set<Integer> selectCourseIndexes(Random random, int courseCount) {
        Set<Integer> selectedCourseIndexes = new HashSet<>(COURSES_PER_STUDENT);
        while (selectedCourseIndexes.size() < COURSES_PER_STUDENT) {
            selectedCourseIndexes.add(random.nextInt(courseCount));
        }

        return selectedCourseIndexes;
    }

    private <T> void flushBatch(List<T> entities, JpaRepository<T, Long> repository) {
        if (entities.size() >= BATCH_SIZE) {
            repository.saveAll(entities);
            repository.flush();
            entities.clear();
        }
    }

    private <T> void flushRemaining(List<T> entities, JpaRepository<T, Long> repository) {
        if (!entities.isEmpty()) {
            repository.saveAll(entities);
            repository.flush();
            entities.clear();
        }
    }
}
