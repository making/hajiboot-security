package com.example;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.example.repository.CustomerRepository;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "logging.level.org.springframework.web.client.RestTemplate=DEBUG",
				"spring.datasource.url=jdbc:h2:mem:customers;DB_CLOSE_ON_EXIT=FALSE" })
@TestMethodOrder(MethodName.class)
class HajibootSecurityApplicationTests {

	@LocalServerPort
	int port;

	WebClient webClient;

	@Autowired
	CustomerRepository customerRepository;

	@BeforeEach
	void init() throws Exception {
		webClient = new WebClient();
		webClient.getOptions().setCssEnabled(false);
		webClient.getOptions().setJavaScriptEnabled(false);
	}

	@AfterEach
	void close() throws Exception {
		webClient.close();
	}

	HtmlPage login(String username, String password) throws Exception {
		HtmlPage login = webClient.getPage("http://localhost:" + port + "/customers");
		String text = login.getBody().getVisibleText();
		assertThat(text).isEqualTo("顧客管理システム\n" + //
				"ログインフォーム\n" + //
				"Sign in");
		HtmlForm form = login.getForms().get(0);
		form.getInputByName("username").setValueAttribute(username);
		form.getInputByName("password").setValueAttribute(password);
		HtmlButton submit = form.getButtonByName("");
		HtmlPage top = submit.click();
		return top;
	}

	@Test
	void step01_login_user1_and_listCustomers() throws Exception {
		HtmlPage top = login("user1", "demo");
		String text = top.getBody().getVisibleText().trim();
		assertThat(text).isEqualTo(
				"顧客管理システム\nuser1さんログイン中。\n顧客情報作成\n姓\n名\n作成\nID 姓 名 担当者 編集\n1 Nobi Nobita user1 \n \n4 Minamoto Shizuka user1 \n \n3 Honekawa Suneo user1 \n \n2 Goda Takeshi user1");
	}

	@Test
	void step02_login_user2_and_listCustomers() throws Exception {
		HtmlPage top = login("user2", "demo");
		String text = top.getBody().getVisibleText().trim();
		assertThat(text).isEqualTo(
				"顧客管理システム\nuser2さんログイン中。\n顧客情報作成\n姓\n名\n作成\nID 姓 名 担当者 編集\n1 Nobi Nobita user1 \n \n4 Minamoto Shizuka user1 \n \n3 Honekawa Suneo user1 \n \n2 Goda Takeshi user1");
	}

	@Test
	void step03_login_failure() throws Exception {
		HtmlPage top = login("user3", "demo");
		String text = top.getBody().getVisibleText().trim();
		assertThat(text).isEqualTo("顧客管理システム\nログインフォーム\nユーザー名またはパスワードが正しくありません。\nSign in");
	}

	@Test
	void step04_createCustomers_error() throws Exception {
		HtmlPage top = login("user2", "demo");
		HtmlForm form = top.getForms().get(1);
		form.getInputByName("firstName").setValueAttribute("");
		form.getInputByName("lastName").setValueAttribute("");
		HtmlButton submit = form.getButtonByName("");
		HtmlPage created = submit.click();

		String text = created.getBody().getVisibleText().trim();
		assertThat(text).isEqualTo(
				"顧客管理システム\nuser2さんログイン中。\n顧客情報作成\n姓\nsize must be between 1 and 127\n名\nsize must be between 1 and 127\n作成\nID 姓 名 担当者 編集\n1 Nobi Nobita user1 \n \n4 Minamoto Shizuka user1 \n \n3 Honekawa Suneo user1 \n \n2 Goda Takeshi user1");
	}

	@Test
	void step05_createCustomers() throws Exception {
		HtmlPage top = login("user2", "demo");
		HtmlForm form = top.getForms().get(1);
		form.getInputByName("firstName").setValueAttribute("Toshiaki");
		form.getInputByName("lastName").setValueAttribute("Maki");
		HtmlButton submit = form.getButtonByName("");
		HtmlPage created = submit.click();

		String text = created.getBody().getVisibleText().trim();
		assertThat(text).isEqualTo(
				"顧客管理システム\nuser2さんログイン中。\n顧客情報作成\n姓\n名\n作成\nID 姓 名 担当者 編集\n1 Nobi Nobita user1 \n \n4 Minamoto Shizuka user1 \n \n3 Honekawa Suneo user1 \n \n2 Goda Takeshi user1 \n \n5 Maki Toshiaki user2");
	}

	@Test
	void step06_editCustomer_error() throws Exception {
		HtmlPage top = login("user2", "demo");
		HtmlForm form = top.getForms().get(2); // Edit Form for customer 1
		HtmlInput submit = form.getInputByName("form");
		HtmlPage editPage = submit.click();

		assertThat(editPage.getUrl().toString())
			.isEqualTo("http://localhost:" + port + "/customers/edit?form=%E7%B7%A8%E9%9B%86&id=1");

		HtmlForm editForm = editPage.getForms().get(1);
		editForm.getInputByName("lastName").setValueAttribute("");
		editForm.getInputByName("firstName").setValueAttribute("");
		HtmlInput edit = editForm.getInputByValue("更新");
		HtmlPage edited = edit.click();

		String text = edited.getBody().getVisibleText().trim();
		assertThat(text).isEqualTo(
				"顧客管理システム\nuser2さんログイン中。\n顧客情報編集\n姓\nsize must be between 1 and 127\n名\nsize must be between 1 and 127");
	}

	@Test
	void step07_editCustomer() throws Exception {
		HtmlPage top = login("user2", "demo");
		HtmlForm form = top.getForms().get(2); // Edit Form for customer 1
		HtmlInput submit = form.getInputByName("form");
		HtmlPage editPage = submit.click();

		assertThat(editPage.getUrl().toString())
			.isEqualTo("http://localhost:" + port + "/customers/edit?form=%E7%B7%A8%E9%9B%86&id=1");

		HtmlForm editForm = editPage.getForms().get(1);
		editForm.getInputByName("lastName").setValueAttribute("Suzuki");
		HtmlInput edit = editForm.getInputByValue("更新");
		HtmlPage edited = edit.click();

		String text = edited.getBody().getVisibleText().trim();
		assertThat(text).startsWith(
				"顧客管理システム\nuser2さんログイン中。\n顧客情報作成\n姓\n名\n作成\nID 姓 名 担当者 編集\n1 Suzuki Nobita user2 \n \n4 Minamoto Shizuka user1 \n \n3 Honekawa Suneo user1 \n \n2 Goda Takeshi user1");
	}

	@Test
	void step08_deleteCustomer() throws Exception {
		HtmlPage top = login("user2", "demo");
		HtmlForm form = top.getForms().get(3); // Delete Form for customer 1
		HtmlInput submit = form.getInputByValue("削除");
		HtmlPage deleted = submit.click();

		String text = deleted.getBody().getVisibleText().trim();
		assertThat(text).startsWith(
				"顧客管理システム\nuser2さんログイン中。\n顧客情報作成\n姓\n名\n作成\nID 姓 名 担当者 編集\n4 Minamoto Shizuka user1 \n \n3 Honekawa Suneo user1 \n \n2 Goda Takeshi user1");
	}

	@Test
	void step09_login_logout() throws Exception {
		HtmlPage top = login("user1", "demo");
		HtmlForm form = top.getForms().get(0); // Delete Form for customer 1
		HtmlInput submit = form.getInputByValue("ログアウト");
		HtmlPage login = submit.click();
		String text = login.getBody().getVisibleText();
		//
		//
		assertThat(text).isEqualTo("顧客管理システム\nログインフォーム\nSign in");
	}

}
