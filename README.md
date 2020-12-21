<p align="center">
  Do you want to build your own Boss with unique features only for your server?
  <a href="https://mineacademy.org/gh-join">
    <img src="https://i.imgur.com/HGc2VG3.png" />
  </a>
</p>

# Boss
Boss is a premium quality plugin for custom monsters on your server.

* Create custom monsters that increase difficulty of your server.
* Use Bosses in monster arenas to challenge your players.
* Clean GUI allows you to configure EVERYTHING in-game easily.
* and much more! Read more [here](https://github.com/kangarko/Boss/wiki/What-is-Boss).

You are welcome to read the **[Boss Wikipedia](https://github.com/kangarko/Boss/wiki)**, where you will find tons of information about the installation, configuring and using this plugin.

If you have any **questions or bugs to report**, you are welcome to **fill an issue**. Please read the [Getting Help the Best Way](https://github.com/kangarko/Boss/wiki/Getting-Help-the-Right-Way) to obtain help as quickly as possible.

# Compiling

1. Obtain Foundation from github.com/kangarko/Foundation
2. Create library/ folder in Boss/ and obtain binaries described in pom.xml. You have to obtain them yourself. Regarding Boss, you can just remove the very few references to it in the source code and remove the dependency from pom.xml.
3. Compile Foundation and Boss using Maven with the "clean install" goal.

<hr>

Dave Thomas, founder of OTI, godfather of the Eclipse strategy:

<i>Clean code can be read, and enhanced by a developer other than its original author. It has unit and acceptance tests. It has meaningful names. It provides one way rather than many ways for doing one thing. It has minimal dependencies, which are explicitly defined, and provides a clear and minimal API. Code should be literate since depending on the language, not all necessary information can be expressed clearly in code alone.</i>
