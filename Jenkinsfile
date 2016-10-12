node {
    // Mark the code checkout 'stage'....
   stage 'Checkout'
   env.PATH = "${tool 'Maven 3'}/bin:${env.PATH}"
   // Checkout code from repository
   checkout scm
   // Run the maven build
   sh 'mvn clean package'
}
