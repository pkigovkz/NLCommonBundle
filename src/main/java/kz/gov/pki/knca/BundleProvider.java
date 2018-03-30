package kz.gov.pki.knca;

import kz.gov.pki.osgi.layer.api.NCALayerService;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import java.security.Provider;

/**
 * Created by Yerlan.Yesmukhanov
 */
public enum BundleProvider {
    KALKAN;

    private static final BundleContext context = FrameworkUtil.getBundle(BundleProvider.class).getBundleContext();
    private NCALayerService ncaLayerService;
    private Provider provider;

    public void discoverProviderService() throws InvalidSyntaxException {
        String serviceFilter = "(objectClass=" + NCALayerService.class.getName() + ")";
        ServiceTracker<NCALayerService, NCALayerService> service_tracker = new ServiceTracker(context, context.createFilter(serviceFilter), null);
        service_tracker.open();
        ncaLayerService = service_tracker.getService();
        if (ncaLayerService == null) {
            context.addServiceListener((e) -> {
                if (e.getType() == ServiceEvent.REGISTERED) {
                    ncaLayerService = service_tracker.getService((ServiceReference<NCALayerService>) e.getServiceReference());
                    provider = ncaLayerService.getProvider();
                }
            }, serviceFilter);
        } else {
            provider = ncaLayerService.getProvider();
        }
    }

    public Provider getProvider(){
        return provider;
    }
}
