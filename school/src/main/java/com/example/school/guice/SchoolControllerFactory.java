package com.example.school.guice;

import com.example.school.controller.SchoolController;
import com.example.school.view.StudentView;

public interface SchoolControllerFactory {
	SchoolController create(StudentView view);

}
