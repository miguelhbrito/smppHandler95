package smppHandler95;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.commons.util.windowing.WindowFuture;
import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.LoggingOptions;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppBindException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppInvalidArgumentException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;

public class App {
	public static Logger log = LoggerFactory.getLogger(App.class);
	
	private static void log(WindowFuture<Integer, PduRequest, PduResponse> future) {
		SubmitSm req = (SubmitSm)future.getRequest();
		SubmitSmResp resp = (SubmitSmResp)future.getResponse();

		log.debug("Got response with MSG ID={} for APPID={}", resp.getMessageId(), req.getReferenceObject());
	}
	public static void main(String[] args) {
		DefaultSmppClient cliente = new DefaultSmppClient();

		SmppSessionConfiguration sessionCfg = new SmppSessionConfiguration();

		sessionCfg.setType(SmppBindType.TRANSCEIVER);
		sessionCfg.setHost("127.0.0.1");
		sessionCfg.setPort(2775);
		sessionCfg.setSystemId("smppclient1");
		sessionCfg.setPassword("password");

		LoggingOptions loggingOpt = new LoggingOptions();

		loggingOpt.setLogPdu(false);
		loggingOpt.setLogBytes(false);

		sessionCfg.setLoggingOptions(loggingOpt);

		try {
			SmppSession session = cliente.bind(sessionCfg, new MySmppSessionHandler());
			SubmitSm sm1 = createSubmitSm("Origem", "Destino", "Texto da msg 1", "UTF-8");
			log.debug("Tentando enviar mensagem 1 !");
			sm1.setReferenceObject("Hello1");
			WindowFuture<Integer, PduRequest, PduResponse> future = session.sendRequestPdu(sm1, TimeUnit.SECONDS.toMillis(60), false);
			
			SubmitSm sm2 = createSubmitSm("Origem", "Destino", "Texto da msg 2", "UTF-8");
			log.debug("Tentando enviar mensagem 2 !");
			sm2.setReferenceObject("Hello2");
			WindowFuture<Integer, PduRequest, PduResponse> future2 = session.sendRequestPdu(sm2, TimeUnit.SECONDS.toMillis(60), false);

			while (!future2.isDone() || !future.isDone()) {
				log.debug("Not done");
			}

			log(future);
			log(future2);
			
			TimeUnit.SECONDS.sleep(10);
			log.debug("Destruindo sessao !");
			session.close();
			session.destroy();

			log.debug("Destruindo cliente !");
			cliente.destroy();

			log.debug("Bye!");
		} catch (SmppTimeoutException ex) {
			log.error("{}", ex);
		} catch (SmppChannelException ex) {
			log.error("{}", ex);
		} catch (SmppBindException ex) {
			log.error("{}", ex);
		} catch (UnrecoverablePduException ex) {
			log.error("{}", ex);
		} catch (InterruptedException ex) {
			log.error("{}", ex);
		} catch (RecoverablePduException ex) {
			log.error("{}", ex);
		}
	}
	
	public static SubmitSm createSubmitSm(String origem, String destino, String msg, String charcode)
			throws SmppInvalidArgumentException {
		SubmitSm sm = new SubmitSm();
		sm.setSourceAddress(new Address((byte) 5, (byte) 0, origem));
		sm.setDestAddress(new Address((byte) 1, (byte) 1, destino));
		sm.setDataCoding((byte) 8);
		sm.setShortMessage(CharsetUtil.encode(msg, charcode));
		sm.setRegisteredDelivery((byte) 1);

		return sm;
	}
}
