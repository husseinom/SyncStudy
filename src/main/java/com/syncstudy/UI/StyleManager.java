package com.syncstudy.UI;

import javafx.scene.Scene;
import java.util.Objects;

/**
 * StyleManager - Gestionnaire centralisé des styles CSS
 *
 * Utilisation:
 *   StyleManager.applyBaseStyles(scene);
 *   StyleManager.applyAdminStyles(scene);
 */
public class StyleManager {

    private static final String STYLES_PATH = "/com/syncstudy/UI/styles/";
    private static final String ADMIN_PATH = "/com/syncstudy/UI/AdminManager/";
    private static final String GROUP_PATH = "/com/syncstudy/UI/GroupManager/";
    private static final String MEMBERSHIP_PATH = "/com/syncstudy/UI/GroupMembership/";

    /**
     * Applique les styles de base (variables, composants, layout)
     * À appeler pour toutes les scènes
     */
    public static void applyBaseStyles(Scene scene) {
        addStylesheet(scene, STYLES_PATH + "base.css");
        addStylesheet(scene, STYLES_PATH + "components.css");
        addStylesheet(scene, STYLES_PATH + "layout.css");
    }

    /**
     * Applique tous les styles de base + le fichier main.css
     */
    public static void applyAllBaseStyles(Scene scene) {
        applyBaseStyles(scene);
        addStylesheet(scene, "/com/syncstudy/UI/main.css");
    }

    /**
     * Applique les styles pour la page de login
     */
    public static void applyLoginStyles(Scene scene) {
        applyBaseStyles(scene);
        addStylesheet(scene, STYLES_PATH + "login.css");
    }

    /**
     * Applique les styles pour le module Admin (UC8)
     */
    public static void applyAdminStyles(Scene scene) {
        applyBaseStyles(scene);
        addStylesheet(scene, ADMIN_PATH + "admin-styles.css");
    }

    /**
     * Applique les styles pour le module Group/Category (UC9)
     */
    public static void applyGroupStyles(Scene scene) {
        applyBaseStyles(scene);
        addStylesheet(scene, GROUP_PATH + "group-styles.css");
    }

    /**
     * Applique les styles pour le module Membership
     */
    public static void applyMembershipStyles(Scene scene) {
        applyBaseStyles(scene);
        addStylesheet(scene, MEMBERSHIP_PATH + "membership-styles.css");
    }

    /**
     * Applique les styles pour le chat
     */
    public static void applyChatStyles(Scene scene) {
        applyBaseStyles(scene);
        addStylesheet(scene, STYLES_PATH + "chat.css");
    }

    /**
     * Applique les styles pour le profil
     */
    public static void applyProfileStyles(Scene scene) {
        applyBaseStyles(scene);
        addStylesheet(scene, STYLES_PATH + "profile.css");
    }

    /**
     * Applique les styles pour les sessions d'étude
     */
    public static void applySessionStyles(Scene scene) {
        applyBaseStyles(scene);
        addStylesheet(scene, STYLES_PATH + "sessions.css");
    }

    /**
     * Applique les styles pour le gestionnaire de fichiers
     */
    public static void applyFileStyles(Scene scene) {
        applyBaseStyles(scene);
        addStylesheet(scene, STYLES_PATH + "files.css");
    }

    /**
     * Applique tous les styles (pour l'application complète)
     */
    public static void applyAllStyles(Scene scene) {
        applyBaseStyles(scene);
        addStylesheet(scene, STYLES_PATH + "login.css");
        addStylesheet(scene, STYLES_PATH + "chat.css");
        addStylesheet(scene, STYLES_PATH + "profile.css");
        addStylesheet(scene, STYLES_PATH + "sessions.css");
        addStylesheet(scene, STYLES_PATH + "files.css");
        addStylesheet(scene, ADMIN_PATH + "admin-styles.css");
        addStylesheet(scene, GROUP_PATH + "group-styles.css");
        addStylesheet(scene, MEMBERSHIP_PATH + "membership-styles.css");
    }

    /**
     * Active le mode sombre
     */
    public static void enableDarkMode(Scene scene) {
        scene.getRoot().getStyleClass().add("dark-mode");
    }

    /**
     * Désactive le mode sombre
     */
    public static void disableDarkMode(Scene scene) {
        scene.getRoot().getStyleClass().remove("dark-mode");
    }

    /**
     * Bascule le mode sombre
     */
    public static void toggleDarkMode(Scene scene) {
        if (scene.getRoot().getStyleClass().contains("dark-mode")) {
            disableDarkMode(scene);
        } else {
            enableDarkMode(scene);
        }
    }

    /**
     * Ajoute une feuille de style à la scène
     */
    private static void addStylesheet(Scene scene, String path) {
        try {
            String css = Objects.requireNonNull(
                StyleManager.class.getResource(path)
            ).toExternalForm();

            if (!scene.getStylesheets().contains(css)) {
                scene.getStylesheets().add(css);
            }
        } catch (NullPointerException e) {
            System.err.println("Warning: Stylesheet not found: " + path);
        }
    }

    /**
     * Supprime toutes les feuilles de style de la scène
     */
    public static void clearStyles(Scene scene) {
        scene.getStylesheets().clear();
    }

    /**
     * Recharge les styles (utile pour le développement)
     */
    public static void reloadStyles(Scene scene) {
        var currentStyles = new java.util.ArrayList<>(scene.getStylesheets());
        scene.getStylesheets().clear();
        scene.getStylesheets().addAll(currentStyles);
    }
}

