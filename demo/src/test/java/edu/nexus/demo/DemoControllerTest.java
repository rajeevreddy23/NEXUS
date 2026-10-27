package edu.nexus.demo;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.sql.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
class DemoControllerTest {
 @Test void databasePoolFaultHoldsAndReleasesRealConnectionHandles()throws Exception{
  var db=mock(DataSource.class);var one=mock(Connection.class);var two=mock(Connection.class);var three=mock(Connection.class);when(db.getConnection()).thenReturn(one,two,three);
  var controller=new DemoController(db,"inventory","",RestClient.builder(),"test-token");controller.fault(new DemoController.Fault("DB_POOL"));
  assertThat(controller.held).hasSize(3);controller.fault(new DemoController.Fault("NONE"));verify(one).close();verify(two).close();verify(three).close();assertThat(controller.held).isEmpty();
 }
 @Test void applicationFaultReturnsFailureAndRecordsEvent()throws Exception{
  var controller=new DemoController(mock(DataSource.class),"inventory","",RestClient.builder(),"test-token");controller.fault(new DemoController.Fault("ERROR"));
  assertThat(controller.work().getStatusCode().value()).isEqualTo(503);assertThat(controller.events()).hasSize(2);
 }
 @Test void unknownFaultModeIsRejectedWithoutChangingState(){
  var controller=new DemoController(mock(DataSource.class),"inventory","",RestClient.builder(),"test-token");assertThatThrownBy(()->controller.fault(new DemoController.Fault("SHELL_COMMAND"))).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);assertThat(controller.mode).isEqualTo(DemoController.Mode.NONE);
 }
}
