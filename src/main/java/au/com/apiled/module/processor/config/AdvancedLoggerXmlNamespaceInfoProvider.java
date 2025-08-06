package au.com.apiled.module.processor.config;

import java.util.Collection;

import org.mule.runtime.dsl.api.xml.XmlNamespaceInfo;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider;

import au.com.apiled.module.api.config.AdvancedLogger;

import static java.util.Arrays.asList;
/**
 * {@link XmlNamespaceInfoProvider} for TEST module.
 *
 * @since 4.0
 */
public class AdvancedLoggerXmlNamespaceInfoProvider implements XmlNamespaceInfoProvider {

  @Override
  public Collection<XmlNamespaceInfo> getXmlNamespacesInfo() {
    return asList(new XmlNamespaceInfo() {

      @Override
      public String getNamespaceUriPrefix() {
        return AdvancedLogger.NAMESPACE;
      }

      @Override
      public String getNamespace() {
        return  AdvancedLogger.PREFIX;
      }
    });
  }
}
