import React, { createContext, useContext, useEffect, useState } from 'react';
import { toast } from 'sonner';

const NotificationContext = createContext();

export const useNotifications = () => {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error('useNotifications must be used within NotificationProvider');
  }
  return context;
};

export const NotificationProvider = ({ children }) => {
  const [socket, setSocket] = useState(null);
  const [notifications, setNotifications] = useState([]);
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    // Initialize WebSocket connection
    const wsUrl = process.env.REACT_APP_BACKEND_URL?.replace('http', 'ws') + '/ws';
    
    try {
      const ws = new WebSocket(wsUrl);
      
      ws.onopen = () => {
        console.log('🔗 WebSocket connected');
        setConnected(true);
        setSocket(ws);
      };

      ws.onmessage = (event) => {
        try {
          const notification = JSON.parse(event.data);
          handleNotification(notification);
        } catch (error) {
          console.error('Error parsing notification:', error);
        }
      };

      ws.onclose = () => {
        console.log('🔌 WebSocket disconnected');
        setConnected(false);
        
        // Attempt to reconnect after 5 seconds
        setTimeout(() => {
          console.log('🔄 Attempting to reconnect...');
          // Re-run this effect
        }, 5000);
      };

      ws.onerror = (error) => {
        console.error('❌ WebSocket error:', error);
        setConnected(false);
      };

      return () => {
        ws.close();
      };
    } catch (error) {
      console.error('Failed to initialize WebSocket:', error);
    }
  }, []);

  const handleNotification = (notification) => {
    console.log('📢 Received notification:', notification);
    
    setNotifications(prev => [notification, ...prev.slice(0, 49)]); // Keep last 50
    
    // Show toast based on notification type
    switch (notification.type) {
      case 'STOCK_LOW':
        toast.warning(notification.title, {
          description: notification.message,
          duration: 5000,
        });
        break;
      case 'STOCK_OUT':
        toast.error(notification.title, {
          description: notification.message,
          duration: 8000,
        });
        break;
      case 'TRANSFER_COMPLETE':
        toast.success(notification.title, {
          description: notification.message,
          duration: 4000,
        });
        break;
      case 'TRANSFER_FAILED':
        toast.error(notification.title, {
          description: notification.message,
          duration: 6000,
        });
        break;
      case 'OPERATION_SUCCESS':
        toast.success(notification.title, {
          description: notification.message,
          duration: 3000,
        });
        break;
      case 'SYSTEM_ERROR':
        toast.error(notification.title, {
          description: notification.message,
          duration: 10000,
        });
        break;
      default:
        toast.info(notification.title, {
          description: notification.message,
          duration: 4000,
        });
    }
  };

  const markAsRead = (notificationId) => {
    setNotifications(prev => 
      prev.map(n => n.id === notificationId ? { ...n, read: true } : n)
    );
  };

  const clearNotifications = () => {
    setNotifications([]);
  };

  const value = {
    notifications,
    connected,
    markAsRead,
    clearNotifications,
    unreadCount: notifications.filter(n => !n.read).length,
  };

  return (
    <NotificationContext.Provider value={value}>
      {children}
    </NotificationContext.Provider>
  );
};