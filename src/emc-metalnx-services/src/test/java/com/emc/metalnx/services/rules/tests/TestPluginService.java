package com.emc.metalnx.services.rules.tests;

import com.emc.metalnx.core.domain.entity.DataGridMSIByServer;
import com.emc.metalnx.core.domain.entity.DataGridMSIPkgInfo;
import com.emc.metalnx.core.domain.entity.DataGridResource;
import com.emc.metalnx.core.domain.entity.DataGridServer;
import com.emc.metalnx.core.domain.exceptions.DataGridConnectionRefusedException;
import com.emc.metalnx.core.domain.exceptions.DataGridException;
import com.emc.metalnx.core.domain.exceptions.DataGridRuleException;
import com.emc.metalnx.services.interfaces.PluginService;
import com.emc.metalnx.services.interfaces.ResourceService;
import com.emc.metalnx.services.interfaces.RuleService;
import com.emc.metalnx.services.irods.PluginServiceImpl;
import org.irods.jargon.core.exception.JargonException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import javax.annotation.PostConstruct;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Test for Rule Service
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-services-context.xml")
@WebAppConfiguration
public class TestPluginService {

    @InjectMocks
    private PluginService pluginService = new PluginServiceImpl();

    @Mock
    private ResourceService mockResourceService;

    @Mock
    private RuleService mockRuleService;

    private static String msiVersion;
    private List<String> msiList, mlxMSIList, iRODSMSIList;

    private List<DataGridServer> servers;

    @PostConstruct
    public void init() {
        servers = new ArrayList<>();
        msiVersion = "1.1.0";

        msiList = new ArrayList<>();
        mlxMSIList = new ArrayList<>();
        iRODSMSIList = new ArrayList<>();

        mlxMSIList.add("libmsiget_illumina_meta.so");
        mlxMSIList.add("libmsiobjget_microservices.so");
        mlxMSIList.add("libmsiobjget_version.so");
        mlxMSIList.add("libmsiobjjpeg_extract.so");
        mlxMSIList.add("libmsiobjput_mdbam.so");
        mlxMSIList.add("libmsiobjput_mdbam.so");
        mlxMSIList.add("libmsiobjput_mdmanifest.so");
        mlxMSIList.add("libmsiobjput_mdvcf.so");
        mlxMSIList.add("libmsiobjput_populate.so");
        iRODSMSIList.add("libmsiobjirods1.so");
        iRODSMSIList.add("libmsiobjirods2.so");

        msiList.addAll(mlxMSIList);
        msiList.addAll(iRODSMSIList);
    }

    @Before
    public void setUp() throws JargonException, DataGridException {
        MockitoAnnotations.initMocks(this);

        DataGridServer s1 = new DataGridServer();
        s1.setHostname("server1.test.com");
        s1.setMSIVersion(msiVersion);
        s1.setIp("192.168.0.1");
        s1.setResources(new ArrayList<>());

        DataGridServer s2 = new DataGridServer();
        s2.setHostname("server2.test.com");
        s2.setMSIVersion(msiVersion);
        s2.setIp("192.168.0.2");
        s2.setResources(new ArrayList<>());

        servers.add(s1);
        servers.add(s2);

        Set<String> expectedMlxMSIList = new HashSet<>();
        expectedMlxMSIList.add("libmsiget_illumina_meta.so");
        expectedMlxMSIList.add("libmsiobjget_microservices.so");
        expectedMlxMSIList.add("libmsiobjget_version.so");
        expectedMlxMSIList.add("libmsiobjjpeg_extract.so");
        expectedMlxMSIList.add("libmsiobjput_mdbam.so");
        expectedMlxMSIList.add("libmsiobjput_mdbam.so");
        expectedMlxMSIList.add("libmsiobjput_mdmanifest.so");
        expectedMlxMSIList.add("libmsiobjput_mdvcf.so");
        expectedMlxMSIList.add("libmsiobjput_populate.so");

        ReflectionTestUtils.setField(pluginService, "msiAPIVersionSupported", msiVersion);
        ReflectionTestUtils.setField(pluginService, "msiMetalnxList", expectedMlxMSIList);
        when(mockResourceService.getAllResourceServers(anyList())).thenReturn(servers);
        when(mockRuleService.execGetVersionRule(anyString())).thenReturn(msiVersion);
        when(mockRuleService.execGetMSIsRule(anyString())).thenReturn(msiList);
    }

    @Test
    public void testMSIInstalledList() throws DataGridConnectionRefusedException {
        DataGridMSIByServer dbMSIByServer = pluginService.getMSIsInstalled("server1.test.com");
        Map<String, Boolean> map = dbMSIByServer.getMetalnxMSIs();
        Set<String> iRODSMSIs = dbMSIByServer.getIRODSMSIs();

        for (String msi: iRODSMSIList) assertTrue(iRODSMSIs.contains(msi));
        for (String msi: mlxMSIList) assertTrue(map.containsKey(msi));
    }

    @Test
    public void testNoPkgMissing() throws DataGridConnectionRefusedException, DataGridRuleException {
        DataGridMSIPkgInfo msiPkgInfo = pluginService.getMSIPkgInfo();
        assertFalse(msiPkgInfo.isThereAnyPkgMissing());
    }

    @Test
    public void testNoPkgNotSupported() throws DataGridConnectionRefusedException, DataGridRuleException {
        DataGridMSIPkgInfo msiPkgInfo = pluginService.getMSIPkgInfo();
        assertFalse(msiPkgInfo.isThereAnyPkgNotSupported());
    }

    @Test
    public void testServers() throws DataGridConnectionRefusedException, DataGridRuleException {
        DataGridMSIPkgInfo msiPkgInfo = pluginService.getMSIPkgInfo();
        assertEquals(2, msiPkgInfo.getServers().size());
        for (DataGridServer server: msiPkgInfo.getServers()) assertEquals(msiVersion, server.getMSIVersion());
    }

    @Test
    public void testMSICompatibility() throws DataGridConnectionRefusedException, DataGridRuleException {
        DataGridResource resc = new DataGridResource();
        resc.setName("demoResc");
        List<DataGridResource> rescs = new ArrayList<>();
        rescs.add(resc);

        servers.get(0).setResources(rescs);

        assertTrue(pluginService.isMSIAPICompatibleInResc("demoResc"));
    }

    @Test
    public void testMSICompatibilityInEmptyResc() throws DataGridConnectionRefusedException, DataGridRuleException {
        assertFalse(pluginService.isMSIAPICompatibleInResc(""));
    }
}