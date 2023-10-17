# Install guide.

## Table of Contents.

- Installing the OS and the necessary packages.

- Preparing the database.

- Downloading and configuring Payara Server 5.x.

- Configure JDBC MySQL connector.

- Configuring Payara Server.

- Installing HMIS.

- Troubleshooting.

- Upgrading HMIS.

## Installing the OS and the necessary packages.

### Installing Ubuntu Server.

Download and install the latest Ubuntu Server in your headless Linux server or a cloud VM (e.g., Ubuntu Server 22.10 x64).

[Get Ubuntu Server | Download | Ubuntu](https://ubuntu.com/download/server)

While installing Ubuntu Server, you can install the minimised version to limit resource usage.

Don't forget to select the box to install `Open SSH Server`.

### Connecting to the Ubuntu Server through SSH.

Using an SSH client, you can log in to the Ubuntu Server.

`ssh USER_NAME@SERVER_IP_ADDRESS`

```shell
ssh admin@192.168.2.129
```

Login to the server by entering your password.

### Installing the necessary packages.

We need `JDK`, `git`, `zip`, `MySql server` and `maven` to complete this task. Therefore we are going to start by installing those packages first.

We need to update the package's database before installing the package.

```bash
sudo apt update
```

And then apply the available upgrades to the Ubuntu server.

```bash
sudo apt upgrade
```

Don't forget to restart your server following the update.

#### Install `JDK`.

It is recommended to install Java Development KIT (`JDK`) 11 to run HMIS on your server.

You can check the already available version of Java with the following command. If you do a fresh install on a Ubuntu server minimised, you will receive a "-bash: Java: command not found" message.

```bash
java --version
```

If you don't have JDK installed, you can install `JDK 11` with the following command.

```bash
sudo apt install openjdk-11-jdk
```

#### Install `git`.

It is recommended to install and use `git` to clone the GitHub repository. Using `git` at this point will make future upgrades easy.

```bash
sudo apt install git
```

#### Install `zip`.

To extract `.zip` archives, we will need a file packaging/archiving utility. For that, install `Info-ZIP` using the following command.

```bash
sudo apt install zip
```

#### Installing `MySql server`.

Need MySQL/MariaDB or similar DBMS to store the data. For this purpose, let's install the `MySql server`.

```bash
sudo apt install mysql-server
```

#### Install Apache Maven.

Apache Maven is a build automation tool used primarily for Java projects. 

```bash
sudo apt install maven
```

Don't forget to restart your server after installing the necessary packages.

## Preparing the database.

Log into the MySQL monitor.

```bash
sudo mysql -u root -p
```

You enter the Ubuntu server admin password for the login until you configure a database root password.

Once you are logged in, configure the root user password by entering the following code into the MySQL command line. You need to replace `ROOT_PASSWORD` with your root password.

```sql
ALTER USER 'root'@'localhost' IDENTIFIED WITH MYSQL_NATIVE_PASSWORD BY 'ROOT_PASSWORD';
```

After configuring the root password, you better create a new user account to access the HMIS database (for security reasons).

You can use the following code in the MySQL command line for that. You need to replace `USER_NAME` and `USER_PASSWORD` with your username and password.

```sql
CREATE USER 'USER_NAME'@'localhost' IDENTIFIED WITH MYSQL_NATIVE_PASSWORD BY 'USER_PASSWORD';
```

After creating the user, create a database for the HMIS. Replace the `DATABASE_NAME` with your database name.

```sql
CREATE DATABASE DATABASE_NAME;
```

Grand user access to the newly created database.

```sql
GRANT ALL PRIVILEGES ON DATABASE_NAME.* TO 'USER_NAME'@'localhost';
```

Exit MySQL monitor.

```sql
EXIT;
```

### Uploading a sample/backup dataset - Optional.

Optionally if you want to upload a sample or a backup dataset, you can do that by following these steps.

- First, upload the backup database to the server via sFTP or any other protocol

- Then log in to the MySql server using the following command and the root password you configured early.

```bash
mysql -u root -p
```

- Then, you can restore the database using the following commands. Need to replace `DATABASE_NAME` and  `./SOURCE_PATH/SOURCE.sql`with the correct values.

```sql
use DATABASE_NAME;

source ./SOURCE_PATH/SOURCE.sql;

Exit;
```

## Downloading and configuring Payara Server 5.x.

We need Payara Server Community version 5.x to run HMIS. We can download it from their website [Payara Platform Community Edition; Payara Services Ltd](https://www.payara.fish/downloads/payara-platform-community-edition/) or [Nexus Repository Manager](https://nexus.payara.fish/#browse/browse:payara-community:fish%2Fpayara%2Fdistributions%2Fpayara%2F5.2022.5%2Fpayara-5.2022.5.zip)

You can download the zip file via a browser to your local computer and then transfer it to your server via `Secure Copy Protocol (SCP)`

`scp LOCAL FILE PATH/FILENAME USER_NAME@SERVER_IP_ADDRESS:SERVER FILE PATH/`

```shell
scp ./payara-5.2022.5.zip admin@192.168.2.129:~/
```

Once the zip archive is uploaded to the server, extract it.

```bash
unzip payara-5.2022.5.zip
```

Once you extract the archive, you can start the Payara Server using the following command.

```bash
./payara5/bin/asadmin start-domain
```

By default, this will start domain1, the default domain included with Payara Server. If you were to create a new domain, that must be specified explicitly. Once the Payara server is started, you will receive a confirmation message.

`Admin Port: 4848`
`Command start-domain executed successfully.`

After starting the Payara Server, you need to configure the admin password and enable the secure admin to access it remotely.  

To change the Payara Server admin password, enter the following command.

```bash
./payara5/bin/asadmin change-admin-password
```

`Enter admin user name [default: admin]>` Type `admin` and enter.
`Enter the admin password>` By default, there is no password. So just press enter.
`Enter the new admin password>` Input your new Payara Server admin password and enter.
`Enter the new admin password again>` Re-input the same password. You will receive a confirmation message once the password is changed.
`Command change-admin-password executed successfully.`

To enable the secure admin to enter the following command.

```shell
./payara5/bin/asadmin enable-secure-admin
```

It will ask for the admin username and password. Use `admin` and the newly created password to authenticate. You will receive the following message after that.

`You must restart all running servers for the change in secure admin to take effect.`
`Command enable-secure-admin executed successfully.`

Now you can restart the Payara Server using the following command.

```shell
./payara5/bin/asadmin restart-domain
```

Once the server is up and running, navigate to `http://SERVER_IP_ADDRESS:4848` in your web browser to access the console.

In this example, `http://192.168.2.129:4848`

You might get a "Your connection is not private" message as we haven't configured SSL certificates yet. You need to ignore that message and proceed to the IP address.

## Configure JDBC MySQL connector.

After configuring the Payara Server, we need to configure the JDBC MySQL connector to connect to the MySQL database.

You can Download the JDBC MySQL connector here: [MySQL:: Download Connector/J/](https://dev.mysql.com/downloads/connector/j)

You must select the Platform Independent (Architecture Independent) and ZIP Archive during download.

You can download the zip file via a browser to your local computer and then transfer it to your server via `Secure Copy Protocol (SCP)`.

`scp LOCAL FILE PATH/FILENAME USER_NAME@SERVER_IP_ADDRESS:SERVER FILE PATH/`

```shell
scp ./mysql-connector-j-8.0.32.zip admin@192.168.2.129:~/
```

Once the zip archive is uploaded into the server, extract it.

```shell
unzip mysql-connector-j-8.0.32.zip
```

Once extracted, you need to add the JDBC MySQL connector to Payara Server using the following command.

```shell
./payara5/bin/asadmin add-library ./mysql-connector-j-8.0.32/mysql-connector-j-8.0.32.jar
```

You need to enter the Payara server admin username and password to authenticate. You will receive a confirmation message once it is done.

`Command add-library executed successfully.`

After adding, you need to restart the Payara server.

```shell
./payara5/bin/asadmin restart-domain
```

## Configuring Payara Server.

After configuring the JDBC MySQL connector, navigate to `http://SERVER_IP_ADDRESS:4848` in your web browser to access the console.

In this example, `http://192.168.2.129:4848`

You might get a "Your connection is not private" message as we haven't configured SSL certificates yet. You need to ignore that message and proceed to the IP address.

Log in to the Payara server administration console with the username admin and the password you created earlier.

### Configuring JDBC Connection Pool.

Go to **Resources** → **JDBC** → **JDBC Connection Pool** → **New**

In the **New JDBC Connection Pool (Step1 of 2)** page, fill the following fields

- **Pool Name:** hmis (A name for your Pool)

- **Resource Type:** javax.sql.DataSource

- **Database Driver Vendor:** MySql8

Then click **Next**

In the **New JDBC Connection Pool (Step 2 of 2) page** 

Scroll down to the **Additional Properties** section

- Delete all the previously created properties

- Then, create the following eight properties

| Name                   | Value                            |
| ---------------------- | -------------------------------- |
| ServerName             | localhost                        |
| PortNumber             | 3306                             |
| DatabaseName           | hmis                             |
| User                   | hmis                             |
| Password               | jZloAkBz2DDp                     |
| UseSSL                 | false                            |
| allowPublicKeyRetrival | true                             |
| URL                    | jdbc:mysql://localhost:3306/hmis |

Once the above properties are filled, click **Finish**

Again open the connection pool we created by clicking its name.

Click **Ping**

If all the configurations are correct, you will get a message saying **✅ Ping Succeeded**

### Configuring JDBC Resources.

After creating the **JDBC Connection Pool**, you need to configure a **JDBC Resource**

Go to **Resources** → **JDBC** → **JDBC Resources** → **New**

- **JNDI Name:** jdbc/hmis

- **Pool Name:** hmis

**Configuration** →  **Server configuration** → **HTTP Service** → **HTTP Listeners** → **http-listener-1**

## Installing HMIS.

### Obtaining the source code.

HMIS source code is available at the official [HIU GitHub repository](https://github.com/lk-gov-health-hiu/hmis)

We can obtain the source code by cloning the GitHub repository branch, which has the production source code.

```bash
git clone https://github.com/hmislk/hmis.git
```

### Packaging and deploying the project.

The following code will package the project into a .`war` file.

```shell
mvn package -f hmis
```

### Deploying the HMIS Application.

To deploy the application, run the following code.

```shell
./payara5/bin/asadmin deploy ./hmis/target/hmis-x.x.war
```

You will be asked for the Payara server admin username and password during the deployment. 

Deployment might take 5 to 10 min, according to your server resources. So please wait patiently.

Once the application is deployed, you will see the following confirmation.

`Application deployed with name hmis-x.x.`
`Command deploy executed successfully.`

After completing the deployment go to Payara Server Console on your web browser.

In the Payara Server Console, go to **Applications**, and under the **Deployed Applications** section, click **Launch** for the deployed application (hmis-x.x). 

It will open a new window, and it will have the links to the launched application. 

`http://sathserver:8080/hmis`

While the application is deployed on a remote server, we must replace the server name with the IP address. 

In this example, `http://192.168.2.129:8080/hmis`

In your web browser, visit the URL, and you will be sent to a page to create the first user and the institution.

After creating the user and the institution, you can log in and use/customise the system.

## Troubleshooting.

If you encounter any problems, please look at the error message and check the log file. You might get a clue to fix that.

Sometimes you may need to edit the following files before deploying.

`./pom.xml`
`./src/main/resources/META-INF/persistence.xml`
`./src/main/webapp/WEB-INF/glassfish-web.xml`

## Upgrading HMIS.

Back up the database and the previous installation.

Change to the hmis directory.

```shell
cd hmis
```

Pull the updated source codes from GitHub.

```shell
git pull
```

Package the project into a .`war` file

```shell
mvn package -f hmis
```

Deploy the new war file.

```shell
./payara5/bin/asadmin deploy --force ./hmis/target/hmis-x.x.war
```
