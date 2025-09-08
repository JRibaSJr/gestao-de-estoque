import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';
import { inventoryAPI, storeAPI, productAPI } from '../services/api';
import { Package, Store, AlertTriangle, TrendingUp, RefreshCw } from 'lucide-react';
import { useNotifications } from '../contexts/NotificationContext';

const Dashboard = () => {
  const [inventory, setInventory] = useState([]);
  const [stores, setStores] = useState([]);
  const [products, setProducts] = useState([]);
  const [lowStock, setLowStock] = useState([]);
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({
    totalProducts: 0,
    totalStores: 0,
    lowStockItems: 0,
    totalValue: 0
  });

  const { notifications, connected, unreadCount } = useNotifications();

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    setLoading(true);
    try {
      const [inventoryRes, storesRes, productsRes, lowStockRes] = await Promise.all([
        inventoryAPI.getAllInventory(),
        storeAPI.getAllStores(),
        productAPI.getAllProducts(),
        inventoryAPI.getLowStock(10)
      ]);

      setInventory(inventoryRes.data);
      setStores(storesRes.data);
      setProducts(productsRes.data);
      setLowStock(lowStockRes.data);

      // Calculate stats
      const totalValue = inventoryRes.data.reduce((sum, item) => 
        sum + (item.quantity * (item.product?.price || 0)), 0
      );

      setStats({
        totalProducts: productsRes.data.length,
        totalStores: storesRes.data.length,
        lowStockItems: lowStockRes.data.length,
        totalValue: totalValue
      });

    } catch (error) {
      console.error('Error loading dashboard data:', error);
    } finally {
      setLoading(false);
    }
  };

  const StatsCard = ({ title, value, icon: Icon, color = "blue", badge }) => (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="text-sm font-medium">{title}</CardTitle>
        <Icon className={`h-4 w-4 text-${color}-600`} />
      </CardHeader>
      <CardContent>
        <div className="text-2xl font-bold">{value}</div>
        {badge && (
          <Badge variant={badge.variant} className="mt-1">
            {badge.text}
          </Badge>
        )}
      </CardContent>
    </Card>
  );

  const InventoryTable = ({ data, title }) => (
    <Card>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b">
                <th className="text-left p-2">Produto</th>
                <th className="text-left p-2">Loja</th>
                <th className="text-left p-2">Estoque</th>
                <th className="text-left p-2">Status</th>
              </tr>
            </thead>
            <tbody>
              {data.slice(0, 10).map((item, index) => (
                <tr key={index} className="border-b">
                  <td className="p-2">
                    <div>
                      <div className="font-medium">{item.product?.name || 'N/A'}</div>
                      <div className="text-gray-500 text-xs">{item.product?.sku}</div>
                    </div>
                  </td>
                  <td className="p-2">{item.store?.name || 'N/A'}</td>
                  <td className="p-2">
                    <Badge variant={item.quantity < 10 ? "destructive" : "secondary"}>
                      {item.quantity}
                    </Badge>
                  </td>
                  <td className="p-2">
                    {item.quantity === 0 ? (
                      <Badge variant="destructive">Sem Estoque</Badge>
                    ) : item.quantity < 10 ? (
                      <Badge variant="destructive">Estoque Baixo</Badge>
                    ) : (
                      <Badge variant="secondary">OK</Badge>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </CardContent>
    </Card>
  );

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <RefreshCw className="h-8 w-8 animate-spin" />
        <span className="ml-2">Carregando dashboard...</span>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold">Dashboard de Inventário</h1>
          <p className="text-gray-600">Sistema Distribuído de Gestão de Estoque</p>
        </div>
        <div className="flex items-center space-x-4">
          <Badge variant={connected ? "default" : "destructive"}>
            {connected ? "Conectado" : "Desconectado"}
          </Badge>
          {unreadCount > 0 && (
            <Badge variant="destructive">{unreadCount} notificações</Badge>
          )}
          <Button onClick={loadDashboardData} size="sm">
            <RefreshCw className="h-4 w-4 mr-2" />
            Atualizar
          </Button>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatsCard
          title="Total de Produtos"
          value={stats.totalProducts}
          icon={Package}
          color="blue"
        />
        <StatsCard
          title="Total de Lojas"
          value={stats.totalStores}
          icon={Store}
          color="green"
        />
        <StatsCard
          title="Itens com Estoque Baixo"
          value={stats.lowStockItems}
          icon={AlertTriangle}
          color="red"
          badge={stats.lowStockItems > 0 ? { variant: "destructive", text: "Atenção" } : null}
        />
        <StatsCard
          title="Valor Total do Estoque"
          value={`R$ ${stats.totalValue.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}`}
          icon={TrendingUp}
          color="purple"
        />
      </div>

      {/* Main Content */}
      <Tabs defaultValue="inventory" className="space-y-4">
        <TabsList>
          <TabsTrigger value="inventory">Inventário Geral</TabsTrigger>
          <TabsTrigger value="low-stock">Estoque Baixo</TabsTrigger>
          <TabsTrigger value="notifications">Notificações</TabsTrigger>
        </TabsList>

        <TabsContent value="inventory">
          <InventoryTable data={inventory} title="Inventário Completo" />
        </TabsContent>

        <TabsContent value="low-stock">
          <InventoryTable data={lowStock} title="Itens com Estoque Baixo" />
        </TabsContent>

        <TabsContent value="notifications">
          <Card>
            <CardHeader>
              <CardTitle>Notificações Recentes</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {notifications.length === 0 ? (
                  <p className="text-gray-500">Nenhuma notificação recente</p>
                ) : (
                  notifications.slice(0, 10).map((notification, index) => (
                    <div key={index} className={`p-3 rounded border ${!notification.read ? 'bg-blue-50' : ''}`}>
                      <div className="flex justify-between items-start">
                        <div>
                          <h4 className="font-medium">{notification.title}</h4>
                          <p className="text-sm text-gray-600">{notification.message}</p>
                        </div>
                        <Badge variant={notification.type === 'SYSTEM_ERROR' ? 'destructive' : 'secondary'}>
                          {notification.type}
                        </Badge>
                      </div>
                      <div className="text-xs text-gray-400 mt-2">
                        {new Date(notification.timestamp).toLocaleString('pt-BR')}
                      </div>
                    </div>
                  ))
                )}
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
};

export default Dashboard;