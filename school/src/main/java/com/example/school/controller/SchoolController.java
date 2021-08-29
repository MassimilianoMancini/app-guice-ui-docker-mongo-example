package com.example.school.controller;

import com.example.school.model.Student;
import com.example.school.repository.StudentRepository;
import com.example.school.view.StudentView;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class SchoolController {

	private StudentView studentView;
	private StudentRepository studentRepository;

	@Inject
	public SchoolController(@Assisted StudentView studentView, StudentRepository studentRepository) {
		this.studentView = studentView;
		this.studentRepository = studentRepository;
	}

	public void allStudents() {
		studentView.showAllStudents(studentRepository.findAll());

	}

	public void newStudent(Student student) {
		Student existingStudent = studentRepository.findById(student.getId());
		if (existingStudent != null) {
			studentView.showError("Already existing student with id " + existingStudent.getId(), existingStudent);
			return;
		}

		studentRepository.save(student);
		studentView.studentAdded(student);

	}

	public void deleteStudent(Student student) {
		if (studentRepository.findById(student.getId()) == null) {
			studentView.showErrorStudentNotFound("No existing student with id " + student.getId(), student);
			return;
		}
		studentRepository.delete(student.getId());
		studentView.studentRemoved(student);

	}
	
}
