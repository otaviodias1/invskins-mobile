import React, { useState, useCallback, useRef, useEffect } from 'react';
import {
    View,
    Text,
    TextInput,
    FlatList,
    TouchableOpacity,
    ActivityIndicator,
    RefreshControl,
    Alert,
    StyleSheet,
} from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import api from '../services/api';
import InventarioItem from '../components/InventarioItem';

export default function InventarioListScreen({ navigation }) {
    const [inventarios, setInventarios] = useState([]);
    const [busca, setBusca] = useState('');
    const [ordenarPor, setOrdenarPor] = useState('nome'); // 'nome' | 'valorTotal'
    const [direcao, setDirecao] = useState('asc'); // 'asc' | 'desc'
    const [loading, setLoading] = useState(false);
    const [refreshing, setRefreshing] = useState(false);
    const [mensagemSucesso, setMensagemSucesso] = useState('');
    const [mensagemErro, setMensagemErro] = useState('');
    const timeoutRef = useRef(null);

    useEffect(() => {
        return () => {
            if (timeoutRef.current) clearTimeout(timeoutRef.current);
        };
    }, []);

    function exibirSucesso(texto) {
        setMensagemErro('');
        setMensagemSucesso(texto);
        if (timeoutRef.current) clearTimeout(timeoutRef.current);
        timeoutRef.current = setTimeout(() => setMensagemSucesso(''), 4000);
    }

    async function carregarInventarios(mostrarLoading = true) {
        try {
            if (mostrarLoading) setLoading(true);
            setMensagemErro('');
            const response = await api.get('/api/inventarios');
            setInventarios(response.data);
        } catch (error) {
            setMensagemErro('Não foi possível carregar os inventários.');
        } finally {
            setLoading(false);
            setRefreshing(false);
        }
    }

    useFocusEffect(
        useCallback(() => {
            carregarInventarios();
        }, [])
    );

    function onRefresh() {
        setRefreshing(true);
        carregarInventarios(false);
    }

    function handleNovo() {
        navigation.navigate('InventarioForm', {
            mode: 'create',
            onSuccess: (msg) => exibirSucesso(msg),
        });
    }

    function handleEditar(item) {
        navigation.navigate('InventarioForm', {
            mode: 'edit',
            inventario: item,
            onSuccess: (msg) => exibirSucesso(msg),
        });
    }

    async function excluirInventario(id) {
        try {
            await api.delete(`/api/inventarios/${id}`);
            setInventarios((prev) => prev.filter((inv) => inv.id !== id));
            exibirSucesso('Inventário excluído com sucesso!');
        } catch (error) {
            setMensagemErro('Não foi possível excluir o inventário.');
        }
    }

    function handleExcluir(item) {
        Alert.alert(
            'Confirmar exclusão',
            `Deseja realmente excluir o inventário "${item.nome}"?`,
            [
                { text: 'Cancelar', style: 'cancel' },
                {
                    text: 'Excluir',
                    style: 'destructive',
                    onPress: () => excluirInventario(item.id),
                },
            ]
        );
    }

    const inventariosFiltrados = inventarios
        .filter((inv) => {
            const termo = busca.trim().toLowerCase();
            if (!termo) return true;
            const nomeMatch = inv.nome?.toLowerCase().includes(termo);
            const descMatch = inv.descricao?.toLowerCase().includes(termo);
            return nomeMatch || descMatch;
        })
        .sort((a, b) => {
            let comparacao = 0;
            if (ordenarPor === 'valorTotal') {
                comparacao = Number(a.valorTotal || 0) - Number(b.valorTotal || 0);
            } else {
                comparacao = (a.nome || '').localeCompare(b.nome || '', 'pt-BR', {
                    sensitivity: 'base',
                });
            }
            return direcao === 'asc' ? comparacao : -comparacao;
        });

    return (
        <View style={styles.container}>
            <View style={styles.header}>
                <Text style={styles.titulo}>Inventários de Skins</Text>
                <TouchableOpacity style={styles.botaoNovo} onPress={handleNovo}>
                    <Text style={styles.textoBotaoNovo}>+ Novo</Text>
                </TouchableOpacity>
            </View>

            <TextInput
                style={styles.inputBusca}
                placeholder="Pesquisar por nome ou descrição..."
                placeholderTextColor="#71717A"
                value={busca}
                onChangeText={setBusca}
            />

            <View style={styles.filtrosRow}>
                <TouchableOpacity
                    style={[styles.chip, ordenarPor === 'nome' && styles.chipAtivo]}
                    onPress={() => setOrdenarPor('nome')}
                >
                    <Text style={styles.chipTexto}>Nome</Text>
                </TouchableOpacity>

                <TouchableOpacity
                    style={[styles.chip, ordenarPor === 'valorTotal' && styles.chipAtivo]}
                    onPress={() => setOrdenarPor('valorTotal')}
                >
                    <Text style={styles.chipTexto}>Valor</Text>
                </TouchableOpacity>

                <TouchableOpacity
                    style={styles.chipDirecao}
                    onPress={() => setDirecao((prev) => (prev === 'asc' ? 'desc' : 'asc'))}
                >
                    <Text style={styles.chipTexto}>
                        {direcao === 'asc' ? 'Crescente ↑' : 'Decrescente ↓'}
                    </Text>
                </TouchableOpacity>
            </View>

            {mensagemSucesso ? (
                <View style={styles.bannerSucesso}>
                    <Text style={styles.textoSucesso}>{mensagemSucesso}</Text>
                </View>
            ) : null}

            {mensagemErro ? (
                <View style={styles.bannerErro}>
                    <Text style={styles.textoErro}>{mensagemErro}</Text>
                </View>
            ) : null}

            {loading ? (
                <ActivityIndicator size="large" color="#2563EB" style={styles.loading} />
            ) : (
                <FlatList
                    data={inventariosFiltrados}
                    keyExtractor={(item) => String(item.id)}
                    renderItem={({ item }) => (
                        <InventarioItem
                            item={item}
                            onEdit={handleEditar}
                            onDelete={handleExcluir}
                        />
                    )}
                    contentContainerStyle={
                        inventariosFiltrados.length === 0 && styles.listaVaziaContainer
                    }
                    ListEmptyComponent={
                        <Text style={styles.textoVazio}>Nenhum inventário encontrado.</Text>
                    }
                    refreshControl={
                        <RefreshControl
                            refreshing={refreshing}
                            onRefresh={onRefresh}
                            tintColor="#FFFFFF"
                        />
                    }
                />
            )}
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#121214',
        padding: 16,
    },
    header: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: 12,
    },
    titulo: {
        color: '#FFFFFF',
        fontSize: 22,
        fontWeight: 'bold',
    },
    botaoNovo: {
        backgroundColor: '#2563EB',
        paddingVertical: 8,
        paddingHorizontal: 14,
        borderRadius: 8,
    },
    textoBotaoNovo: {
        color: '#FFFFFF',
        fontWeight: '600',
        fontSize: 14,
    },
    inputBusca: {
        backgroundColor: '#1E1E24',
        color: '#FFFFFF',
        borderRadius: 8,
        paddingHorizontal: 12,
        paddingVertical: 10,
        borderWidth: 1,
        borderColor: '#27272A',
        marginBottom: 10,
    },
    filtrosRow: {
        flexDirection: 'row',
        gap: 8,
        marginBottom: 12,
    },
    chip: {
        backgroundColor: '#1E1E24',
        paddingVertical: 6,
        paddingHorizontal: 12,
        borderRadius: 6,
        borderWidth: 1,
        borderColor: '#27272A',
    },
    chipAtivo: {
        backgroundColor: '#2563EB',
        borderColor: '#2563EB',
    },
    chipDirecao: {
        backgroundColor: '#27272A',
        paddingVertical: 6,
        paddingHorizontal: 12,
        borderRadius: 6,
        marginLeft: 'auto',
    },
    chipTexto: {
        color: '#FFFFFF',
        fontSize: 12,
        fontWeight: '600',
    },
    bannerSucesso: {
        backgroundColor: '#064E3B',
        borderColor: '#10B981',
        borderWidth: 1,
        borderRadius: 8,
        padding: 10,
        marginBottom: 10,
    },
    textoSucesso: {
        color: '#A7F3D0',
        fontSize: 13,
        fontWeight: '500',
    },
    bannerErro: {
        backgroundColor: '#7F1D1D',
        borderColor: '#EF4444',
        borderWidth: 1,
        borderRadius: 8,
        padding: 10,
        marginBottom: 10,
    },
    textoErro: {
        color: '#FECACA',
        fontSize: 13,
        fontWeight: '500',
    },
    loading: {
        marginTop: 40,
    },
    listaVaziaContainer: {
        flexGrow: 1,
        justifyContent: 'center',
        alignItems: 'center',
    },
    textoVazio: {
        color: '#71717A',
        fontSize: 15,
    },
});