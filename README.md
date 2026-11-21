# SmartBank-frontend

Interface desktop SmartBanking en JavaFX, Java 17 minimum et Maven. Le code a été déplacé depuis SmartBank-App en conservant ses packages Java et ses chemins de ressources.

## Structure

```text
src/main/java/ma/bankdirect/
  App.java                 Démarrage et fenêtre
  PrimaryController.java   Écran de connexion
  SecondaryController.java Ancienne vue de démonstration
src/main/resources/com/bankapp/
  primary.fxml             Connexion desktop
  secondary.fxml           Ancienne vue de démonstration
  auth.css                 Charte turquoise / bleu profond
```

## Lancer

```bash
mvn clean compile
mvn javafx:run
```

Dans le workspace complet, le lanceur PowerShell `../SmartBank-App/scripts/run-desktop.ps1` peut aussi être utilisé avec un runtime JavaFX local. `JAVA_HOME` doit désigner un JDK 17 ou supérieur.

## Limites actuelles

L’interface valide la saisie, masque ou affiche le mot de passe et affiche les informations d’aide. La connexion réelle, les sessions et la récupération automatique restent à raccorder au backend. Aucun accès MySQL direct, identifiant SQL ou modèle métier backend n’est nécessaire au démarrage de cette interface.

Les maquettes, le cahier des charges personnel et les sources temporaires de vérification graphique sont conservés dans `../Livrables-locaux/`, hors des dépôts. Les dossiers `output/`, `target/` et `tmp/` sont ignorés par Git.
