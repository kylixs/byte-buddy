package net.bytebuddy.agent;

import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.test.utility.UnixSocketRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.io.File;
import java.io.FileOutputStream;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class VirtualMachineForHotSpotOnPosixTest {

    private static final String FOO = "foo";
    
    @Rule
    public MethodRule unixSocketRule = new UnixSocketRule();

    private File agent;

    @Before
    public void setUp() throws Exception {
        agent = File.createTempFile("testagent", ".jar");
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
        manifest.getMainAttributes().putValue("Agent-Class", SampleAgent.class.getName());
        JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(agent), manifest);
        try {
            outputStream.putNextEntry(new JarEntry(SampleAgent.class.getName().replace('.', '/') + ".class"));
            outputStream.write(ClassFileLocator.ForClassLoader.read(SampleAgent.class));
            outputStream.closeEntry();
        } finally {
            outputStream.close();
        }
    }

    @After
    public void tearDown() throws Exception {
        SampleAgent.argument = null;
    }

    @Test
    public void canAttachViaPosixSocket() throws Exception {
        VirtualMachine virtualMachine = VirtualMachine.ForHotSpot.OnPosix.attach(ByteBuddyAgent.ProcessProvider.ForCurrentVm.INSTANCE.resolve());
        try {
            virtualMachine.loadAgent(agent.getAbsolutePath(), FOO);
        } finally {
            virtualMachine.detach();
        }
        assertThat(SampleAgent.argument, is(FOO));
    }
}
