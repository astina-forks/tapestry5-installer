package com.spreadthesource.tapestry.installer.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.ioc.internal.util.TapestryException;
import org.apache.tapestry5.ioc.services.RegistryShutdownListener;

import com.spreadthesource.tapestry.installer.InstallerConstants;

public class ApplicationSettingsImpl implements ApplicationSettings, RegistryShutdownListener
{
    private final Properties properties;

    private final File config;

    private final boolean alreadyInstalled;

    /**
     * Prepare properties with existing or create a new one.
     * 
     * @param appName
     * @param installerVersion
     * @throws FileNotFoundException
     */
    public ApplicationSettingsImpl(
            @Inject @Symbol(InstallerConstants.CONFIGURATION_FILEPATH) String configFilePath,
            @Inject @Symbol(SymbolConstants.APPLICATION_VERSION) String installerVersion)
            throws IOException
    {
        this.properties = new Properties();
        
        File propertyFile = new File(configFilePath);
        
        if(propertyFile.getParentFile() == null) {
            throw new IllegalArgumentException("Configuration path should not be created in root folder !");
        }
        
        if (!propertyFile.exists())
        {
            if (propertyFile.getParentFile().canWrite() && propertyFile.getParentFile().canRead())
            {
                this.config = propertyFile;
                this.properties.put(InstallerConstants.INSTALLER_VERSION, installerVersion);
                this.alreadyInstalled = false;
            }
            else
            {
                throw new IOException("Cannot write nor read the configuration in "
                        + propertyFile.getParentFile());
            }
        }
        else
        {
            // Load
            this.config = propertyFile;
            this.load();

            // Check installer version
            if (this.properties.containsKey(InstallerConstants.INSTALLER_VERSION))
            {
                if (this.properties.get(InstallerConstants.INSTALLER_VERSION).equals(
                        installerVersion))
                {
                    this.alreadyInstalled = true;
                }
                else
                {
                    // Set the new version and keep the previous one
                    this.properties.put(
                            InstallerConstants.PREVIOUS_INSTALLER_VERSION,
                            this.properties.get(InstallerConstants.INSTALLER_VERSION));
                    this.properties.put(InstallerConstants.INSTALLER_VERSION, installerVersion);
                    this.alreadyInstalled = false;
                }
            }
            else
            {
                throw new IllegalStateException(
                        String
                                .format(
                                        "Cannot find installer version, configuration file '%s' is maybe corrupt.",
                                        this.config.getPath()));
            }
        }

    }

    public String get(String key)
    {
        return properties.getProperty(key);
    }


    public boolean containsKey(String key)
    {
        return properties.containsKey(key);
    }

    public String valueForSymbol(String symbolName)
    {
        return get(symbolName);
    }

    public void put(String key, String value)
    {
        if (value == null)
            value = "";
        
        properties.put(key, value);
    }

    /**
     * The properties will be stored to the disk.
     */
    public void registryDidShutdown()
    {
        FileWriter fos = null;
        try
        {
            fos = new FileWriter(config);
            properties.store(fos, null);
        }
        catch (Exception e)
        {
            throw new TapestryException("Error writing configuration to disk", e);
        }
        finally
        {
            if (fos != null)
            {
                try
                {
                    fos.close();
                }
                catch (IOException e)
                {
                    throw new TapestryException("Error writing configuration to disk", e);
                }
            }
        }

    }

    /**
     * Simply load existing properties and let the server restart
     * 
     * @throws FileNotFoundException
     * @throws IOException
     */
    private void load() throws FileNotFoundException, IOException
    {
        if (config.canWrite() && config.canRead())
        {
            FileInputStream fis = new FileInputStream(config);
            try
            {
                properties.load(fis);
            }
            finally
            {
                if (fis != null)
                {
                    fis.close();
                }
            }
        }
        else
        {
            throw new FileNotFoundException("Cannot write nor read the configuration in "
                    + config.getAbsolutePath());
        }
    }


    public boolean isAlreadyInstalled()
    {
        return alreadyInstalled;
    }

}
