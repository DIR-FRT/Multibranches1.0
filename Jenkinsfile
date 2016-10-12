node {
    // Mark the code checkout 'stage'....
   stage 'Checkout'
   git url: "https://github.com/DIR-FRT/Multibranches1.0.git"
   env.PATH = "${tool 'Maven 3'}/bin:${env.PATH}"
   // Checkout code from repository
   checkout scm
   // Run the maven build
   sh 'mvn clean package'
}
