package com.dell.rti4t.xd.jmx;

import java.util.Hashtable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.naming.ObjectNamingStrategy;
import org.springframework.jmx.support.ObjectNameManager;

import com.dell.rti4t.xd.eventhandler.AbstractEventHandlerFactory;

public class VFROMetricsMBeanExporter extends MBeanExporter {
	
	static final String domain = "dca";

	private ObjectNamingStrategy namingStrategy = new ObjectNamingStrategy() {
		@Override
		public ObjectName getObjectName(Object managedBean, String beanKey) throws MalformedObjectNameException {
			VFROJMXExposedMetrics vfROJMXMetrics = (VFROJMXExposedMetrics)managedBean;
			Hashtable<String, String> properties = new Hashtable<String, String>();
			properties.put("type", vfROJMXMetrics.type());
			properties.put("name", vfROJMXMetrics.name());
			properties.put("identity", vfROJMXMetrics.identity());
			ObjectName returnedObject = ObjectNameManager.getInstance(domain, properties);
			return returnedObject;
		}
	};
	
	@Autowired
	List<AbstractEventHandlerFactory> metricExposers;
	
	@Autowired
	MBeanServer mbeanServer;
	
	@PostConstruct
	public void registerAllBeans() {
		setServer(mbeanServer);
		setEnsureUniqueRuntimeObjectNames(false);
		setNamingStrategy(namingStrategy);
		for(AbstractEventHandlerFactory metricExposer : metricExposers) {
			if(metricExposer.registerBean()) {
				add(metricExposer.getVFROMetrics());
			}
		}
	}

	private void add(VFROInputOutputMetrics vfROMetrics) {
		if(vfROMetrics != null) {
			registerManagedResource(new VFROJMXExposedMetrics(vfROMetrics));
		}
	}
}
