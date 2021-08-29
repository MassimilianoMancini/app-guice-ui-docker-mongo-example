package com.example.school.view.swing;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;

import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.runner.GUITestRunner;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.example.school.controller.SchoolController;
import com.example.school.guice.SchoolSwingMongoDefaultModule;
import com.example.school.model.Student;
import com.example.school.repository.mongo.StudentMongoRepository;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;

@RunWith(GUITestRunner.class)
public class StudentSwingViewIT extends AssertJSwingJUnitTestCase {
	private static MongoServer server;
	private static InetSocketAddress serverAddress;

	@Inject
	private MongoClient mongoClient;
	
	@Inject
	private StudentSwingView studentSwingView;
	
	@Inject
	private StudentMongoRepository studentRepository;
	
	private SchoolController schoolController;
	private FrameFixture window;
	
	@BeforeClass
	public static void setupServer() {
		server = new MongoServer(new MemoryBackend());
		// bind on a random local port
		serverAddress = server.bind();
	}

	@AfterClass
	public static void shutdownServer() {
		server.shutdown();
	}

	@Override
	protected void onSetUp() throws Exception {
		final Module moduleForTesting = 
			Modules
				.override(new SchoolSwingMongoDefaultModule())
				.with(new AbstractModule() {
					@Override
					public void configure() {
						bind(MongoClient.class)
							.toInstance(new MongoClient(new ServerAddress(serverAddress)));
					}
				});
		
		final Injector injector = Guice.createInjector(moduleForTesting);
		
		GuiActionRunner.execute(() -> {
			injector.injectMembers(this);
			for (Student student : studentRepository.findAll()) {
				studentRepository.delete(student.getId());
			}
			schoolController = studentSwingView.getSchoolController();
			return studentSwingView;
		});
		window = new FrameFixture(robot(), studentSwingView);
		window.show();
	}

	@Override
	protected void onTearDown() {
		mongoClient.close();
	}

	@Test
	public void testAllStudents() {
		// use repository to add students to the database
		Student student1 = new Student("1", "test1");
		Student student2 = new Student("2", "test2");
		studentRepository.save(student1);
		studentRepository.save(student2);
		// use the controller's allStudents
		GuiActionRunner.execute(() -> schoolController.allStudents());
		// and verify that the view's list is populated
		assertThat(window.list().contents()).containsExactly("1 - test1", "2 - test2");
	}

	@Test
	public void testAddButtonSuccess() {
		window.textBox("idTextBox").enterText("1");
		window.textBox("nameTextBox").enterText("test");
		window.button(JButtonMatcher.withText("Add")).click();
		assertThat(window.list().contents()).containsExactly("1 - test");
	}

	@Test
	public void testAddButtonError() {
		studentRepository.save(new Student("1", "existing"));
		window.textBox("idTextBox").enterText("1");
		window.textBox("nameTextBox").enterText("test");
		window.button(JButtonMatcher.withText("Add")).click();
		assertThat(window.list().contents()).isEmpty();
		window.label("errorMessageLabel")
				.requireText("Already existing student with id 1: 1 - existing");
	}

	@Test
	public void testDeleteButtonSuccess() {
		// use the controller to populate the view's list...
		GuiActionRunner.execute(() -> schoolController.newStudent(new Student("1", "toremove")));
		window.list().selectItem(0);
		window.button(JButtonMatcher.withText("Delete Selected")).click();
		assertThat(window.list().contents()).isEmpty();
	}

	@Test
	public void testDeleteButtonError() {
		// manually add a student to the list, which will not be in the db
		Student student = new Student("1", "non existent");
		GuiActionRunner.execute(() -> studentSwingView.getListStudentsModel().addElement(student));
		window.list().selectItem(0);
		window.button(JButtonMatcher.withText("Delete Selected")).click();
		assertThat(window.list().contents()).isEmpty();
		window.label("errorMessageLabel").requireText("No existing student with id 1: 1 - non existent");

	}

}
