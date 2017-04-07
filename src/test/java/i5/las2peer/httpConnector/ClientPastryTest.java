package i5.las2peer.httpConnector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import i5.las2peer.httpConnector.client.AccessDeniedException;
import i5.las2peer.httpConnector.client.Client;
import i5.las2peer.p2p.LocalNode;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.p2p.ServiceNameVersion;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.testing.TestSuite;
import i5.las2peer.tools.SimpleTools;

public class ClientPastryTest {

	public static final ServiceNameVersion testServiceClass = new ServiceNameVersion(
			i5.las2peer.testing.TestService.class.getName(), "1.0");

	private PastryNodeImpl node;
	private HttpConnector connector;

	@Before
	public void setup() {
		try {
			// start a launcher
			node = TestSuite.launchNetwork(1).get(0);

			UserAgent agent = MockAgentFactory.getEve();
			agent.unlockPrivateKey("evespass");
			node.storeAgent(agent);
			agent = MockAgentFactory.getAdam();
			agent.unlockPrivateKey("adamspass");
			node.storeAgent(agent);

			connector = new HttpConnector();
			connector.setPort(38080);
			connector.start(node);

			String passPhrase = SimpleTools.createRandomString(20);

			ServiceAgent myAgent = ServiceAgent.createServiceAgent(testServiceClass, passPhrase);
			myAgent.unlockPrivateKey(passPhrase);

			node.registerReceiver(myAgent);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@After
	public void tearDown() {
		try {
			connector.stop();
			node.shutDown();

			connector = null;
			node = null;

			LocalNode.reset();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void test() {
		try {
			UserAgent adam = MockAgentFactory.getAdam();

			System.out.println("adam: " + adam.getId());

			Client c = new Client("localhost", 38080, "" + adam.getId(), "adamspass");
			c.setSessionTimeout(1000);
			c.connect();

			c.invoke(testServiceClass.getName(), "storeEnvelopeString", "ein test");

			Object result = c.invoke(testServiceClass.getName(), "getEnvelopeString", new Object[0]);

			assertEquals("ein test", result);

			Thread.sleep(500);
			c.disconnect();

			UserAgent eve = MockAgentFactory.getEve();
			System.out.println("eve: " + eve.getId());

			Client c2 = new Client("localhost", 38080, "" + eve.getId(), "evespass");
			c.setSessionOutdate(3000);
			c.connect();

			try {
				result = c2.invoke(testServiceClass.getName(), "getEnvelopeString", new Object[0]);
				fail("AccessDeniedException expected");
			} catch (AccessDeniedException e) {
			}
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

}
