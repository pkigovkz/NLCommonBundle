package kz.gov.pki.knca;

import kz.gov.pki.knca.types.ResponseMessage;
import kz.gov.pki.osgi.layer.api.ModuleService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osgi.framework.*;
import org.osgi.service.log.LogService;
import java.lang.reflect.Method;
import java.util.Hashtable;
import static kz.gov.pki.knca.BundleLog.LOG;
import static kz.gov.pki.knca.BundleProvider.KALKAN;

/**
 * Created by Yerlan.Yesmukhanov.
 */
public class NCALayerCommonActivator implements BundleActivator
{
    private static final int[] ARRAY_INT = new int[]{};
    private static final String[] ARRAY_STRING = new String[]{};

    public void start(BundleContext context) throws Exception {
        try{
            LOG.discoverLogService();
            KALKAN.discoverProviderService();

            CommonUtils commonUtils = new CommonUtils();
            Hashtable<String, String> props = new Hashtable<String, String>();
            props.put("module", "kz.gov.pki.knca.commonUtils");
            context.registerService(ModuleService.class.getName().toString(), getModuleService(commonUtils) , props);

        } catch (Exception e) {
            LOG.log(LogService.LOG_ERROR, e.getMessage(), e);
            throw new Exception("Can not start bundle NCALayerCommon!");
        }
    }

    public void stop(BundleContext context) throws Exception {
    }

    private ModuleService getModuleService(CommonUtils commonUtils){
        return (jsonString, headers) -> {
            try {
                JSONObject jsonObject = new JSONObject(jsonString);
                String uuid = jsonObject.optString("uuid");
                String mname = jsonObject.getString("method");
                JSONArray args = jsonObject.optJSONArray("args");

                int argLen = args != null ? args.length() : 0;

                Method method = null;
                if (!mname.isEmpty()) {
                    Method[] ms = commonUtils.getClass().getDeclaredMethods();
                    for (Method m : ms) {
                        if (m.getName().equals(mname) && m.getParameterTypes().length == argLen) {
                            method = m;
                            break;
                        }
                    }
                }
                if (method != null) {
                    Object argObjs[] = null;
                    if (argLen > 0) {
                        argObjs = new Object[argLen];
                    }
                    for (int i = 0; i < argLen; i++) {
                        Object objArg = args.get(i);
                        if (objArg instanceof JSONArray) {
                            JSONArray arrayObj = ((JSONArray) objArg);
                            if (method.getParameterTypes()[i].isInstance(ARRAY_INT)) {
                                int[] arrayArg = new int[arrayObj.length()];
                                for (int j = 0; j < arrayArg.length; j++) {
                                    arrayArg[j] = arrayObj.getInt(j);
                                }
                                argObjs[i] = arrayArg;
                            } else if (method.getParameterTypes()[i].isInstance(ARRAY_STRING)) {
                                String[] arrayArg = new String[arrayObj.length()];
                                for (int j = 0; j < arrayArg.length; j++) {
                                    arrayArg[j] = arrayObj.getString(j);
                                }
                                argObjs[i] = arrayArg;
                            } else {
                                Object[] arrayArg = new Object[arrayObj.length()];
                                for (int j = 0; j < arrayArg.length; j++) {
                                    arrayArg[j] = arrayObj.get(j);
                                }
                                argObjs[i] = arrayArg;
                            }
                        } else {
                            argObjs[i] = objArg;
                        }
                    }
                    String jsonResult = null;
                    try {
                        jsonResult = (String) method.invoke(commonUtils, argObjs);
                    } catch (Exception e) {
                        LOG.log(LogService.LOG_ERROR, e.getMessage(), e);
                        return getErrorResponse(e + ". Method: " + mname);
                    }

                    if (uuid.isEmpty()) {
                        return jsonResult;
                    } else {
                        JSONObject resultObject = new JSONObject(jsonResult);
                        return resultObject.put("uuid", uuid).toString();
                    }
                } else {
                    return getErrorResponse("NoSuchMethodException " + mname);
                }
            } catch (Exception e) {
                LOG.log(LogService.LOG_ERROR, e.getMessage(), e);
                return getErrorResponse(e.getMessage());
            }
        };
    }


    private String getErrorResponse(String msg) {
        ResponseMessage responseForJS = new ResponseMessage("500");
        responseForJS.setMessage(msg);
        return ((JSONObject) JSONObject.wrap(responseForJS)).toString();
    }
}
