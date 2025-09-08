import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Badge } from '../components/ui/badge';
import { inventoryAPI, storeAPI, productAPI } from '../services/api';
import { Package, Plus, Minus, ArrowRightLeft, Search } from 'lucide-react';
import { toast } from 'sonner';

const Inventory = () => {
  const [inventory, setInventory] = useState([]);
  const [stores, setStores] = useState([]);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedStore, setSelectedStore] = useState('all');
  
  // Modal states
  const [showStockModal, setShowStockModal] = useState(false);
  const [showTransferModal, setShowTransferModal] = useState(false);
  const [selectedItem, setSelectedItem] = useState(null);
  const [modalData, setModalData] = useState({
    quantity: '',
    referenceId: '',
    notes: '',
    toStoreId: ''
  });

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      const [inventoryRes, storesRes, productsRes] = await Promise.all([
        inventoryAPI.getAllInventory(),
        storeAPI.getAllStores(),
        productAPI.getAllProducts()
      ]);
      
      setInventory(inventoryRes.data);
      setStores(storesRes.data);
      setProducts(productsRes.data);
    } catch (error) {
      toast.error('Erro ao carregar dados');
      console.error('Error loading data:', error);
    } finally {
      setLoading(false);
    }
  };

  const filteredInventory = inventory.filter(item => {
    const matchesSearch = !searchTerm || 
      item.product?.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      item.product?.sku.toLowerCase().includes(searchTerm.toLowerCase()) ||
      item.store?.name.toLowerCase().includes(searchTerm.toLowerCase());
    
    const matchesStore = selectedStore === 'all' || 
      item.store?.id.toString() === selectedStore;
    
    return matchesSearch && matchesStore;
  });

  const handleStockOperation = async (operation) => {
    try {
      const data = {
        storeId: selectedItem.store.id,
        productId: selectedItem.product.id,
        quantity: parseInt(modalData.quantity),
        referenceId: modalData.referenceId,
        notes: modalData.notes
      };

      if (operation === 'in') {
        await inventoryAPI.stockIn(data);
        toast.success('Entrada de estoque realizada com sucesso');
      } else {
        await inventoryAPI.stockOut(data);
        toast.success('Saída de estoque realizada com sucesso');
      }
      
      setShowStockModal(false);
      resetModalData();
      await loadData();
    } catch (error) {
      toast.error('Erro na operação de estoque');
      console.error('Error in stock operation:', error);
    }
  };

  const handleTransfer = async () => {
    try {
      const data = {
        fromStoreId: selectedItem.store.id,
        toStoreId: parseInt(modalData.toStoreId),
        productId: selectedItem.product.id,
        quantity: parseInt(modalData.quantity),
        notes: modalData.notes
      };

      await inventoryAPI.transferInventory(data);
      toast.success('Transferência iniciada com sucesso');
      
      setShowTransferModal(false);
      resetModalData();
      await loadData();
    } catch (error) {
      toast.error('Erro na transferência');
      console.error('Error in transfer:', error);
    }
  };

  const resetModalData = () => {
    setModalData({
      quantity: '',
      referenceId: '',
      notes: '',
      toStoreId: ''
    });
    setSelectedItem(null);
  };

  const openStockModal = (item, operation) => {
    setSelectedItem(item);
    setShowStockModal(operation);
    setModalData({ ...modalData, quantity: '', referenceId: '', notes: '' });
  };

  const openTransferModal = (item) => {
    setSelectedItem(item);
    setShowTransferModal(true);
    setModalData({ ...modalData, quantity: '', toStoreId: '', notes: '' });
  };

  if (loading) {
    return <div className="flex items-center justify-center h-64">Carregando...</div>;
  }

  return (
    <div className="p-6 space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold">Gestão de Inventário</h1>
        <Button onClick={loadData}>Atualizar</Button>
      </div>

      {/* Filters */}
      <Card>
        <CardContent className="p-4">
          <div className="flex gap-4 items-center">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-3 h-4 w-4 text-gray-400" />
              <Input
                placeholder="Buscar por produto, SKU ou loja..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-10"
              />
            </div>
            <select
              value={selectedStore}
              onChange={(e) => setSelectedStore(e.target.value)}
              className="h-10 px-3 py-2 border border-gray-300 rounded-md"
            >
              <option value="all">Todas as Lojas</option>
              {stores.map(store => (
                <option key={store.id} value={store.id.toString()}>
                  {store.name}
                </option>
              ))}
            </select>
          </div>
        </CardContent>
      </Card>

      {/* Inventory Table */}
      <Card>
        <CardHeader>
          <CardTitle>Inventário ({filteredInventory.length} itens)</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b">
                  <th className="text-left p-3">Produto</th>
                  <th className="text-left p-3">Loja</th>
                  <th className="text-left p-3">Estoque</th>
                  <th className="text-left p-3">Valor Unit.</th>
                  <th className="text-left p-3">Status</th>
                  <th className="text-left p-3">Ações</th>
                </tr>
              </thead>
              <tbody>
                {filteredInventory.map((item, index) => (
                  <tr key={index} className="border-b hover:bg-gray-50">
                    <td className="p-3">
                      <div>
                        <div className="font-medium">{item.product?.name}</div>
                        <div className="text-gray-500 text-xs">{item.product?.sku}</div>
                      </div>
                    </td>
                    <td className="p-3">{item.store?.name}</td>
                    <td className="p-3">
                      <Badge variant={item.quantity < 10 ? "destructive" : "secondary"}>
                        {item.quantity}
                      </Badge>
                    </td>
                    <td className="p-3">
                      R$ {(item.product?.price || 0).toFixed(2)}
                    </td>
                    <td className="p-3">
                      {item.quantity === 0 ? (
                        <Badge variant="destructive">Sem Estoque</Badge>
                      ) : item.quantity < 10 ? (
                        <Badge variant="destructive">Baixo</Badge>
                      ) : (
                        <Badge variant="secondary">OK</Badge>
                      )}
                    </td>
                    <td className="p-3">
                      <div className="flex gap-2">
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => openStockModal(item, 'in')}
                        >
                          <Plus className="h-4 w-4" />
                        </Button>
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => openStockModal(item, 'out')}
                          disabled={item.quantity === 0}
                        >
                          <Minus className="h-4 w-4" />
                        </Button>
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => openTransferModal(item)}
                          disabled={item.quantity === 0}
                        >
                          <ArrowRightLeft className="h-4 w-4" />
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>

      {/* Stock In/Out Modal */}
      {showStockModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <Card className="w-96">
            <CardHeader>
              <CardTitle>
                {showStockModal === 'in' ? 'Entrada de Estoque' : 'Saída de Estoque'}
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <strong>Produto:</strong> {selectedItem?.product?.name}
              </div>
              <div>
                <strong>Loja:</strong> {selectedItem?.store?.name}
              </div>
              <div>
                <strong>Estoque Atual:</strong> {selectedItem?.quantity}
              </div>
              
              <Input
                placeholder="Quantidade"
                type="number"
                value={modalData.quantity}
                onChange={(e) => setModalData({...modalData, quantity: e.target.value})}
              />
              
              <Input
                placeholder="Referência (opcional)"
                value={modalData.referenceId}
                onChange={(e) => setModalData({...modalData, referenceId: e.target.value})}
              />
              
              <Input
                placeholder="Observações (opcional)"
                value={modalData.notes}
                onChange={(e) => setModalData({...modalData, notes: e.target.value})}
              />
              
              <div className="flex gap-2">
                <Button
                  onClick={() => handleStockOperation(showStockModal)}
                  disabled={!modalData.quantity}
                >
                  Confirmar
                </Button>
                <Button
                  variant="outline"
                  onClick={() => setShowStockModal(false)}
                >
                  Cancelar
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Transfer Modal */}
      {showTransferModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <Card className="w-96">
            <CardHeader>
              <CardTitle>Transferir Estoque</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <strong>Produto:</strong> {selectedItem?.product?.name}
              </div>
              <div>
                <strong>Da Loja:</strong> {selectedItem?.store?.name}
              </div>
              <div>
                <strong>Disponível:</strong> {selectedItem?.quantity}
              </div>
              
              <select
                value={modalData.toStoreId}
                onChange={(e) => setModalData({...modalData, toStoreId: e.target.value})}
                className="w-full h-10 px-3 py-2 border border-gray-300 rounded-md"
              >
                <option value="">Selecione a loja destino</option>
                {stores
                  .filter(store => store.id !== selectedItem?.store?.id)
                  .map(store => (
                    <option key={store.id} value={store.id}>
                      {store.name}
                    </option>
                  ))
                }
              </select>
              
              <Input
                placeholder="Quantidade"
                type="number"
                max={selectedItem?.quantity}
                value={modalData.quantity}
                onChange={(e) => setModalData({...modalData, quantity: e.target.value})}
              />
              
              <Input
                placeholder="Observações (opcional)"
                value={modalData.notes}
                onChange={(e) => setModalData({...modalData, notes: e.target.value})}
              />
              
              <div className="flex gap-2">
                <Button
                  onClick={handleTransfer}
                  disabled={!modalData.quantity || !modalData.toStoreId}
                >
                  Transferir
                </Button>
                <Button
                  variant="outline"
                  onClick={() => setShowTransferModal(false)}
                >
                  Cancelar
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
};

export default Inventory;