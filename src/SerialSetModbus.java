//import gnu.io.CommPortIdentifier;
import javax.comm.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JFrame;

import com.focus_sw.fieldtalk.MbusRtuMasterProtocol;
import com.focus_sw.fieldtalk.MbusSerialMasterProtocol;



public class SerialSetModbus extends Thread {

	protected JComboBox cbPorts, cbPrefix, cbAddress, cbSpeed;
	protected JTextField tfSerialNumber;
	protected Vector<String> ports, address, speed;
	protected String serialPort;
	
	protected MbusRtuMasterProtocol mbusDevice;
	
	protected JFrame fSerialNumber;
	
	public static void main(String[] args) {
		(new SerialSetModbus()).start();
	}
	
	SerialSetModbus () {
		mbusDevice = new MbusRtuMasterProtocol();		
		mbusDevice.setRetryCnt(2); // Increase to 2 for poor links
		mbusDevice.setPollDelay(0); // Increase if slave needs time between polls

	}
	
	protected void writeSerial() {
		int serialPrefix, serialNumber;
		int mbusSpeed, mbusAddress;
		
		
		serialPort = ((String) cbPorts.getSelectedItem());
		serialPrefix = ((String) cbPrefix.getSelectedItem()).charAt(0);
		serialNumber = Integer.parseInt(tfSerialNumber.getText());
		
		mbusSpeed = Integer.parseInt((String) cbSpeed.getSelectedItem());
		
		String parts[] = ((String) cbAddress.getSelectedItem()).split(" ");
//		System.err.println("# parts of string = { ");
//		for ( int i=0 ; i<parts.length ; i++ )
//			System.err.printf("\t[%d] %s",i,parts[i]);
		mbusAddress=Integer.parseInt(parts[0].trim());
		
		System.err.println("# SerialPrefix=" + serialPrefix + " SerialNumber=" + serialNumber);
		System.err.println("# mbusAddress=" + mbusAddress + " mbusSpeed=" + mbusSpeed);

		try {
			System.err.println("# Opening Modbus device");
			mbusDevice.openProtocol(serialPort, mbusSpeed,	MbusSerialMasterProtocol.DATABITS_8, MbusSerialMasterProtocol.STOPBITS_2, MbusSerialMasterProtocol.PARITY_NONE);

			/* unlock device */
			System.err.println("# Unlocking factory registers Modbus device");
			mbusDevice.writeSingleRegister(mbusAddress, 20000, (short) 1802);
			Thread.sleep(100);

			/* write serial prefix */
			System.err.println("# Writing serial prefix to Modbus device");
			mbusDevice.writeSingleRegister(mbusAddress, 1001, (short) serialPrefix);
			Thread.sleep(100);
			
			/* write serial number */
			System.err.println("# Writing serial number to Modbus device");
			mbusDevice.writeSingleRegister(mbusAddress, 1002, (short) serialNumber);
			Thread.sleep(100);
			
			/* write to flash */
			System.err.println("# Writing flash on Modbus device");
			mbusDevice.writeSingleRegister(mbusAddress, 2000, (short) 1);
			Thread.sleep(250);
			
			System.err.println("# Closing connect to Modbus device");
			mbusDevice.closeProtocol();
		} catch ( Exception e ) {
			System.err.println("# Caught exception while setting serial number:\n" + e);
			JOptionPane.showMessageDialog(fSerialNumber,
				    "Exception: " + e,
				    "Error Setting Serial Number",
				    JOptionPane.ERROR_MESSAGE);

		}

		
	}
	
	protected void screenSetSerial() {
		fSerialNumber = new JFrame("APRS World Serial Number Set");
		fSerialNumber.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		fSerialNumber.setSize(350, 350);

		/* Overall BorderLayout */
		Container cont = fSerialNumber.getContentPane();
		cont.setBackground(Color.white);
		//cont.setLayout(new BorderLayout());
		cont.setLayout(new GridLayout(0, 1));
		
		
		System.out.println("# Scanning serial ports: ");
		/* Get all available ports */
		Enumeration<?> portIdentifiers = CommPortIdentifier.getPortIdentifiers();

		/* Find requested port */
		ports = new Vector<String>();
		while (portIdentifiers.hasMoreElements()) {
			CommPortIdentifier p = (CommPortIdentifier)portIdentifiers.nextElement();
			
			if (p.getPortType() == CommPortIdentifier.PORT_SERIAL ) { 
				System.out.println("Available port=" +p.getName());
				ports.add(p.getName());
			}
		}

		fSerialNumber.add(new JLabel("Serial Port:"));
		cbPorts = new JComboBox(ports);
		fSerialNumber.add(cbPorts);

		speed = new Vector<String>();
		speed.add(new String("9600"));
		speed.add(new String("19200"));
		speed.add(new String("57600"));
		speed.add(new String("115200"));
		fSerialNumber.add(new JLabel("Serial Speed:"));
		cbSpeed = new JComboBox(speed);
		fSerialNumber.add(cbSpeed);
		
		address = new Vector<String>();
		address.add(new String("24 (XRW2G)"));
		address.add(new String("25 (MagWeb)"));
		address.add(new String("26 (SDC)"));
		address.add(new String("27 (PS2Tap)"));
		address.add(new String("28 (SLHC)"));
		address.add(new String("29 (WTIC / NEXUCOM)"));
		address.add(new String("30 (ShuntMonitor)"));
		address.add(new String("31 (WrenDAQ4)"));
		address.add(new String("32 (WatchdogArlo)"));
		address.add(new String("33 (POE Monitor)"));
		address.add(new String("34 (Crane Wind Monitor)"));
		address.add(new String("35 (SPOT Controller)"));
		address.add(new String("36 (PDist LP Modbus)"));
		address.add(new String("37 (DC Switch Controller)"));
		address.add(new String("38 (PiCameraWeatherX)"));
		/* not sure what is address 39. Or if 38 is a duplicate */
		address.add(new String("40 (pdist48)"));
		
		fSerialNumber.add(new JLabel("Modbus Address:"));
		cbAddress = new JComboBox(address);
		fSerialNumber.add(cbAddress);

		

		Vector<String> vSerialPrefixes = new Vector<String>();
		vSerialPrefixes.add("A");
		vSerialPrefixes.add("M");
		vSerialPrefixes.add("Z");
		

		fSerialNumber.add(new JLabel("Serial Prefix:"));
		cbPrefix = new JComboBox(vSerialPrefixes);
		fSerialNumber.add(cbPrefix);
	
		fSerialNumber.add(new JLabel("Serial Number:"));
		tfSerialNumber = new JTextField();
		fSerialNumber.add(tfSerialNumber);
		
	
		JButton bInstall = new JButton("Set Now");
		fSerialNumber.add(bInstall);
		bInstall.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						writeSerial();
						System.err.println("# Clicked on set serial number");
					}
				}
		);
		
		
		fSerialNumber.setVisible(true);
	}
	
	
	public void run() {
		screenSetSerial();
	}
}
